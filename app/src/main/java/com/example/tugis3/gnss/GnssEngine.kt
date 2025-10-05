package com.example.tugis3.gnss

import com.example.tugis3.Tugis3Application
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import com.example.tugis3.gnss.model.FixType
import com.example.tugis3.gnss.model.GnssObservation
import com.example.tugis3.gnss.nmea.NmeaParser
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Hafif GNSS motoru: Android LocationManager üzerinden temel konum + uydu bilgisi yayınlar.
 * İleride NTRIP düzeltmelerini entegre edebilmek için genişletilebilir.
 */
class GnssEngine(
    private val locationManager: LocationManager
) : LocationListener {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainScope = CoroutineScope(Dispatchers.Main) // GNSS callback kayıtları ana threadde
    private var running = false
    private var gnssJob: Job? = null
    private var degradeJob: Job? = null
    private val degradeCheckIntervalMs = 5_000L
    private val degradeThresholdFloat = 20_000L // 20s sonra RTK_FIX -> RTK_FLOAT
    private val degradeThresholdDgps = 40_000L  // 40s sonra RTK_FLOAT -> DGPS
    private val degradeThresholdSingle = 80_000L // 80s sonra DGPS -> SINGLE

    private val _observation = MutableStateFlow<GnssObservation?>(null)
    val observation: StateFlow<GnssObservation?> = _observation.asStateFlow()

    private var satellitesInUse: Int = 0
    private var satellitesVisible: Int = 0

    private val nmeaParser = NmeaParser()
    private var lastNmea: GnssObservation? = null

    // Son 3 fix kalitesinin 3 saniyelik kayan penceresi (RTK sabitleme kararlılığı için)
    private val recentFixQualities = mutableListOf<Pair<Long, FixType>>()

    private val _lastCorrectionMs = MutableStateFlow<Long?>(null)
    val lastCorrectionMs: StateFlow<Long?> = _lastCorrectionMs.asStateFlow()

    // RTCM iyileştirme eşiği: belirli bir pencere içinde yeterli düzeltme gelmeden fix yükseltme
    private var correctionWindowStart = 0L
    private var correctionCountInWindow = 0
    private val correctionWindowMs = 10_000L
    private val requiredCorrectionsForUpgrade = 3

    private val statusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            satellitesVisible = status.satelliteCount
            satellitesInUse = (0 until status.satelliteCount).count { status.usedInFix(it) }
            // Uydu detaylarını topla
            val list = mutableListOf<SatelliteInfo>()
            for (i in 0 until status.satelliteCount) {
                val type = when(status.getConstellationType(i)) {
                    GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                    GnssStatus.CONSTELLATION_GALILEO -> "GAL"
                    GnssStatus.CONSTELLATION_BEIDOU -> "BDS"
                    GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                    GnssStatus.CONSTELLATION_SBAS -> "SBAS"
                    GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
                    GnssStatus.CONSTELLATION_UNKNOWN -> "UNK"
                    else -> "GPS"
                }
                list.add(
                    SatelliteInfo(
                        svid = status.getSvid(i),
                        constellation = type,
                        azimuthDeg = status.getAzimuthDegrees(i),
                        elevationDeg = status.getElevationDegrees(i),
                        cn0DbHz = status.getCn0DbHz(i),
                        usedInFix = status.usedInFix(i)
                    )
                )
            }
            _satellites.value = list
        }
    }

    fun start() {
        if (running) return
        running = true
        // Eski yaklaşımda coroutine context kayması nedeniyle bazı cihazlarda Handler main looper olmadan oluşturulup crash oluyordu.
        // Her durumda ana looper üzerinden handler ile kayıt yap.
        val mainLooper = android.os.Looper.getMainLooper()
        val handler = android.os.Handler(mainLooper)
        gnssJob = mainScope.launch {
            try {
                // API seviyesinden bağımsız olarak handler parametresiyle kaydet (tutarlılık için)
                runCatching { locationManager.unregisterGnssStatusCallback(statusCallback) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // R ve üzeri için handler parametresi olan overload mevcut değilse fallback
                    // Öncelikle doğrudan çağır, başarısız olursa handler ile eski overload'ı dene
                    val direct = runCatching { locationManager.registerGnssStatusCallback(statusCallback) }
                    if (direct.isFailure) {
                        runCatching { locationManager.registerGnssStatusCallback(statusCallback, handler) }
                    }
                } else {
                    locationManager.registerGnssStatusCallback(statusCallback, handler)
                }
                // Konum güncellemeleri ana looper üzerinden
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 500L, 0f, this@GnssEngine, mainLooper
                )
            } catch (e: SecurityException) {
                // izin yok -> motoru durdur
                running = false
            } catch (t: Throwable) {
                running = false
            }
        }
        degradeJob = scope.launch { degradeLoop() }
    }

    fun stop() {
        if (!running) return
        running = false
        mainScope.launch {
            runCatching {
                locationManager.removeUpdates(this@GnssEngine)
                locationManager.unregisterGnssStatusCallback(statusCallback)
            }
        }
        gnssJob?.cancel(); degradeJob?.cancel()
    }

    private suspend fun degradeLoop() {
        while (running) {
            delay(degradeCheckIntervalMs)
            val lastCorr = _lastCorrectionMs.value ?: continue
            val age = System.currentTimeMillis() - lastCorr
            val current = _observation.value ?: continue
            val newFix = when {
                current.fixType == FixType.RTK_FIX && age > degradeThresholdFloat -> FixType.RTK_FLOAT
                current.fixType == FixType.RTK_FLOAT && age > degradeThresholdDgps -> FixType.DGPS
                current.fixType == FixType.DGPS && age > degradeThresholdSingle -> FixType.SINGLE
                else -> null
            }
            if (newFix != null) {
                _observation.value = current.copy(fixType = newFix)
            }
        }
    }

    data class SatelliteInfo(
        val svid: Int,
        val constellation: String,
        val azimuthDeg: Float,
        val elevationDeg: Float,
        val cn0DbHz: Float,
        val usedInFix: Boolean
    )

    private val _satellites = MutableStateFlow<List<SatelliteInfo>>(emptyList())
    val satellites: StateFlow<List<SatelliteInfo>> = _satellites.asStateFlow()

    private val _nmeaLineCount = MutableStateFlow(0L)
    val nmeaLineCount: StateFlow<Long> = _nmeaLineCount.asStateFlow()

    fun ingestNmea(line: String) {
        _nmeaLineCount.value = _nmeaLineCount.value + 1
        if (NmeaLogConfig.enabled) {
            runCatching {
                val dir = File(Tugis3Application.appContext.filesDir, "logs")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "nmea.log")
                f.appendText(line + "\n")
            }.getOrElse { }
        }
        val parsed = nmeaParser.parse(line) ?: return
        val base = _observation.value

        // Fix kalitesi değerlendirmesi için GGA satırlarındaki fix değerlerini takip et
        if (line.contains("GGA") && parsed.fixType != null) {
            val now = System.currentTimeMillis()
            // 3 saniyeden eski girdileri sil
            recentFixQualities.removeAll { (timestamp, _) -> now - timestamp > 3000 }
            // Yeni fix'i ekle
            recentFixQualities.add(Pair(now, parsed.fixType))
        }

        // Fix tipini belirle
        val mergedFix = if (parsed.fixType != null) {
            if (parsed.fixType == FixType.RTK_FIX &&
                recentFixQualities.count { it.second == FixType.RTK_FIX } >= 2) {
                FixType.RTK_FIX
            } else {
                parsed.fixType
            }
        } else {
            base?.fixType ?: FixType.SINGLE
        }

        val effectiveSatUsed = parsed.satellitesInUse ?: base?.satellitesInUse ?: satellitesInUse
        if (parsed.satellitesVisible != null) satellitesVisible = parsed.satellitesVisible
        val hdopVal = parsed.hdop ?: base?.hdop
        val pdopVal = parsed.pdop ?: base?.pdop
        val vdopVal = parsed.vdop ?: base?.vdop

        // Geliştirilmiş doğruluk modeli: Fix tipine göre farklı temel değerler kullan
        val fixModifier = when(mergedFix) {
            FixType.RTK_FIX -> 0.01
            FixType.RTK_FLOAT -> 0.1
            FixType.DGPS -> 0.3
            FixType.PPP -> 0.2
            else -> 1.0
        }

        // Temel doğruluk parametreleri
        val baseH = 0.5 * fixModifier
        val baseV = 0.8 * fixModifier

        // HRMS/VRMS hesaplama formülleri (geliştirme - daha gerçekçi sonuçlar)
        val hrmsEstimate = when {
            hdopVal != null -> hdopVal * baseH
            pdopVal != null -> (pdopVal * baseH * 0.7)
            else -> base?.hrms
        }

        val vrmsEstimate = when {
            vdopVal != null -> vdopVal * baseV
            pdopVal != null && hdopVal != null -> sqrt((pdopVal*pdopVal - hdopVal*hdopVal).coerceAtLeast(0.25)) * baseV
            hdopVal != null -> hdopVal * baseV * 1.2
            hrmsEstimate != null -> hrmsEstimate * 1.5
            else -> base?.vrms
        }

        val merged = GnssObservation(
            epochMillis = System.currentTimeMillis(),
            latDeg = parsed.latDeg ?: base?.latDeg,
            lonDeg = parsed.lonDeg ?: base?.lonDeg,
            ellipsoidalHeight = parsed.heightEllipsoidal ?: base?.ellipsoidalHeight,
            fixType = mergedFix,
            satellitesInUse = effectiveSatUsed,
            satellitesVisible = satellitesVisible,
            hrms = hrmsEstimate ?: base?.hrms,
            vrms = vrmsEstimate ?: base?.vrms,
            pdop = pdopVal,
            hdop = hdopVal,
            vdop = vdopVal,
            rawNmea = line
        )
        lastNmea = merged
        _observation.value = merged
    }

    override fun onLocationChanged(location: Location) {
        // Android konum nesnesinden fix kalitesini çıkarabildiğimizce çıkar
        val fix = when {
            lastNmea?.fixType?.isDifferential() == true -> lastNmea?.fixType ?: FixType.SINGLE
            (location.extras?.getInt("satellites", 0) ?: 0) <= 3 -> FixType.SINGLE
            location.accuracy > 10 -> FixType.SINGLE
            location.accuracy <= 1.0 -> FixType.DGPS
            else -> lastNmea?.fixType ?: FixType.SINGLE
        }
        val baseNmea = lastNmea
        val verticalAcc: Double? = if (Build.VERSION.SDK_INT >= 26) {
            val v = location.verticalAccuracyMeters
            if (v > 0) v.toDouble() else null
        } else null
        val obs = GnssObservation(
            epochMillis = System.currentTimeMillis(),
            latDeg = location.latitude,
            lonDeg = location.longitude,
            ellipsoidalHeight = location.altitude,
            fixType = fix,
            satellitesInUse = satellitesInUse,
            satellitesVisible = satellitesVisible,
            hrms = location.accuracy.toDouble(),
            vrms = verticalAcc ?: baseNmea?.vrms,
            pdop = baseNmea?.pdop,
            hdop = baseNmea?.hdop,
            vdop = baseNmea?.vdop,
            rawNmea = baseNmea?.rawNmea
        )
        _observation.value = obs
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    /**
     * NTRIP’den gelen RTCM düzeltmesini uygular (simüle).
     * Gerçek donanım SDK’sı olmadığından sadece metrikleri iyileştirip fix tipini yükseltebilir.
     */
    fun applyCorrection(rtcm: ByteArray) {
        _lastCorrectionMs.value = System.currentTimeMillis()
        val now = System.currentTimeMillis()
        if (now - correctionWindowStart > correctionWindowMs) {
            correctionWindowStart = now
            correctionCountInWindow = 0
        }
        correctionCountInWindow++
        val current = _observation.value ?: return
        if (correctionCountInWindow < requiredCorrectionsForUpgrade) return
        // Eşik aşıldıysa sayaç sıfırlansın ki kademeli ilerlesin
        correctionCountInWindow = 0
        correctionWindowStart = now
        val newFix = when (current.fixType) {
            FixType.NO_FIX, FixType.SINGLE -> FixType.DGPS
            FixType.DGPS -> FixType.RTK_FLOAT
            FixType.RTK_FLOAT -> FixType.RTK_FIX
            FixType.RTK_FIX, FixType.PPP, FixType.MANUAL -> current.fixType
        }
        if (newFix != current.fixType) {
            val improvedHrms = current.hrms?.let { (it * 0.6).coerceAtMost(0.02) } ?: 0.05
            _observation.value = current.copy(
                fixType = newFix,
                hrms = improvedHrms
            )
        }
    }
}
