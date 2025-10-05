package com.example.tugis3.ntrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.NtripProfileRepository
import com.example.tugis3.data.db.entity.NtripProfileEntity
import com.example.tugis3.gnss.GnssEngine
import com.example.tugis3.ntrip.NtripClient.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlin.system.measureTimeMillis
import java.util.concurrent.atomic.AtomicLong
import com.example.tugis3.data.repository.NtripSessionRepository
import com.example.tugis3.data.db.entity.NtripSessionEntity
import com.example.tugis3.gnss.model.FixType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@HiltViewModel
class NtripProfilesViewModel @Inject constructor(
    private val repo: NtripProfileRepository,
    private val client: NtripClient,
    private val gnssEngine: GnssEngine,
    private val sessionRepo: NtripSessionRepository
) : ViewModel() {

    private var autoConnectAttempted = false
    private var retryCount = 0
    private val maxRetries = 3

    private val _connectionStatus = MutableStateFlow("Bağlı değil")
    val connectionStatusFlow: StateFlow<String> = _connectionStatus
    val connectionStatus: String get() = connectionStatusFlow.value

    private val _connectedProfileId = MutableStateFlow<Long?>(null)
    val connectedProfileId: StateFlow<Long?> = _connectedProfileId

    val profiles: StateFlow<List<NtripProfileUi>> = repo.observeProfiles()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val nmeaBuffer = StringBuilder()

    private val _rtcmBytes = MutableStateFlow(0L)
    val rtcmBytes: StateFlow<Long> = _rtcmBytes
    private val _nmeaBytes = MutableStateFlow(0L)
    val nmeaBytes: StateFlow<Long> = _nmeaBytes
    private val _lastRtcmTimestamp = MutableStateFlow<Long?>(null)
    val lastRtcmTimestamp: StateFlow<Long?> = _lastRtcmTimestamp
    private val _isSimulated = MutableStateFlow<Boolean>(false)
    val isSimulated: StateFlow<Boolean> = _isSimulated
    private val _dataRateBps = MutableStateFlow(0.0)
    val dataRateBps: StateFlow<Double> = _dataRateBps

    private val _diffAgeSec = MutableStateFlow<Long?>(null)
    val diffAgeSec: StateFlow<Long?> = _diffAgeSec
    private val _rtcmTypeCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val rtcmTypeCounts: StateFlow<Map<Int, Int>> = _rtcmTypeCounts

    private var sessionStartTs: Long? = null
    private var sessionStartRtcm = 0L
    private var sessionStartNmea = 0L
    private var maxRate = 0.0
    private var correctionPackets = 0

    private var diffAgeJob: Job? = null

    private var windowStart: Long = System.currentTimeMillis()
    private var windowBytes: Int = 0

    init {
        viewModelScope.launch {
            client.events.collect { ev ->
                _connectionStatus.value = when (ev) {
                    is Event.Connecting -> "Bağlanıyor..."
                    is Event.Connected -> { retryCount = 0; "Bağlandı" }
                    is Event.Data -> { processIncomingBytes(ev.bytes); "Veri alınıyor (${ev.bytes.size}B)" }
                    is Event.Error -> {
                        val msg = "Hata: ${ev.message}"
                        _connectedProfileId.value = null
                        // Retry only if there is an autoConnect profile & retry limit not exceeded
                        attemptRetryIfAuto()
                        msg
                    }
                    is Event.Stopped -> {
                        _connectedProfileId.value = null
                        "Durduruldu"
                    }
                }
            }
        }
        // Auto-connect logic
        viewModelScope.launch {
            profiles.collect { list ->
                if (!autoConnectAttempted) {
                    val auto = list.firstOrNull { it.autoConnect }
                    if (auto != null) {
                        autoConnectAttempted = true
                        connect(auto)
                    }
                }
            }
        }
        // GNSS pozisyonunu NTRIP client'a besle
        viewModelScope.launch {
            gnssEngine.observation.collect { obs ->
                obs?.let {
                    val fixQ = when (it.fixType) {
                        FixType.RTK_FIX -> 4
                        FixType.RTK_FLOAT -> 5
                        FixType.DGPS -> 2
                        FixType.PPP -> 6
                        FixType.SINGLE, FixType.NO_FIX, FixType.MANUAL -> 1
                    }
                    if (it.latDeg != null && it.lonDeg != null) {
                        client.updatePosition(
                            it.latDeg, it.lonDeg, it.ellipsoidalHeight, fixQ, it.satellitesInUse ?: 0
                        )
                    }
                }
            }
        }
        // Diff age ticker
        diffAgeJob = viewModelScope.launch {
            while (true) {
                val ts = lastRtcmTimestamp.value
                _diffAgeSec.value = ts?.let { (System.currentTimeMillis() - it) / 1000 } ?: null
                delay(1000)
            }
        }
        // Max rate izleme
        viewModelScope.launch {
            dataRateBps.collect { rate -> if (rate > maxRate) maxRate = rate }
        }
    }

    override fun onCleared() {
        diffAgeJob?.cancel()
        super.onCleared()
    }

    private fun attemptRetryIfAuto() {
        val auto = profiles.value.firstOrNull { it.autoConnect } ?: return
        if (retryCount >= maxRetries) return
        retryCount++
        viewModelScope.launch {
            // small backoff
            kotlinx.coroutines.delay(1500L * retryCount)
            // Only retry if still not connected
            if (_connectedProfileId.value == null && !_connectionStatus.value.startsWith("Bağlandı")) {
                connect(auto)
            }
        }
    }

    private fun processIncomingBytes(raw: ByteArray) {
        if (NtripClient.isRtcmMessage(raw)) {
            _rtcmBytes.value += raw.size
            _lastRtcmTimestamp.value = System.currentTimeMillis()
            accumulateRate(raw.size)
            gnssEngine.applyCorrection(raw)
            correctionPackets++
            // RTCM Type kaba çıkarım: D3 | len hi | len lo | msg type bits
            if (raw.size > 3 && raw[0].toInt() and 0xFF == 0xD3) {
                // RTCM3: message number ilk 12 bit header sonrası 24 bit block'ta yer alır; burada basitleştirilmiş
                // raw[3] ve raw[4]'ün üst 6 + alt 4 bit kombinasyonu: (b3 << 4) | (b4 >> 4)
                val b3 = raw.getOrNull(3)?.toInt() ?: 0
                val b4 = raw.getOrNull(4)?.toInt() ?: 0
                val msgType = ((b3 and 0xFF) shl 4) or ((b4 ushr 4) and 0x0F)
                _rtcmTypeCounts.update { old ->
                    val cur = old[msgType] ?: 0
                    old + (msgType to (cur + 1))
                }
            }
            return
        }
        // Muhtemel çoklu NMEA chunk'ı içinde satır kırıkları olabilir
        for (b in raw) {
            val c = b.toInt() and 0xFF
            if (c == 0x0A || c == 0x0D) {
                if (nmeaBuffer.isNotEmpty()) {
                    val line = nmeaBuffer.toString().trim()
                    nmeaBuffer.clear()
                    if (line.startsWith('$') && line.length > 6) {
                        _nmeaBytes.value += line.length
                        accumulateRate(line.length)
                        gnssEngine.ingestNmea(line)
                    }
                }
            } else if (c in 0x20..0x7E || c == '$'.code.toInt()) {
                if (nmeaBuffer.length < 1500) nmeaBuffer.append(c.toChar()) else nmeaBuffer.clear()
            }
        }
    }

    private fun accumulateRate(added: Int) {
        windowBytes += added
        val now = System.currentTimeMillis()
        val elapsed = now - windowStart
        if (elapsed >= 2000) { // 2 sn pencere
            _dataRateBps.value = (windowBytes * 1000.0) / elapsed
            windowStart = now
            windowBytes = 0
        }
    }

    fun save(ui: NtripProfileUi) = viewModelScope.launch {
        val id = repo.upsert(ui.toEntity())
        if (ui.autoConnect) repo.setAutoConnect(ui.copy(id = id, autoConnect = true).toEntity())
    }

    fun delete(ui: NtripProfileUi) = viewModelScope.launch {
        repo.delete(ui.toEntity())
        if (connectionStatus == "Bağlandı") client.stop()
    }

    fun toggleAuto(ui: NtripProfileUi) = viewModelScope.launch {
        val newState = !ui.autoConnect
        if (newState) {
            repo.setAutoConnect(ui.copy(autoConnect = true).toEntity())
            repo.upsert(ui.copy(autoConnect = true).toEntity())
        } else {
            repo.upsert(ui.copy(autoConnect = false).toEntity())
        }
    }

    fun connect(ui: NtripProfileUi) = viewModelScope.launch {
        if (_connectedProfileId.value == ui.id && connectionStatus.startsWith("Bağ")) {
            finalizeSession()
            client.stop(); return@launch
        }
        _connectedProfileId.value = ui.id
        client.stop()
        _rtcmBytes.value = 0L; _nmeaBytes.value = 0L; _dataRateBps.value = 0.0
        sessionStartTs = System.currentTimeMillis()
        sessionStartRtcm = 0L
        sessionStartNmea = 0L
        maxRate = 0.0
        correctionPackets = 0
        _rtcmTypeCounts.value = emptyMap()
        val simulateMode = ui.host.equals("demo", true) || ui.host.isBlank() || ui.mountPoint.isBlank()
        _isSimulated.value = simulateMode
        client.start(
            NtripClient.Config(
                host = ui.host,
                port = ui.port,
                mountPoint = ui.mountPoint,
                username = ui.username,
                password = ui.password,
                simulate = simulateMode
            )
        )
    }

    fun stop() { finalizeSession(); _connectedProfileId.value = null; client.stop(); _isSimulated.value = false }

    private fun finalizeSession() {
        val start = sessionStartTs ?: return
        val end = System.currentTimeMillis()
        val rtcm = _rtcmBytes.value
        val nmea = _nmeaBytes.value
        val avgRate = if (end > start) (rtcm + nmea).toDouble() * 1000.0 / (end - start) else 0.0
        val profileId = _connectedProfileId.value
        val fix = gnssEngine.observation.value?.fixType?.name
        viewModelScope.launch {
            runCatching {
                sessionRepo.logSession(
                    NtripSessionEntity(
                        profileId = profileId,
                        startTs = start,
                        endTs = end,
                        rtcmBytes = rtcm,
                        nmeaBytes = nmea,
                        avgRateBps = avgRate,
                        maxRateBps = maxRate,
                        corrections = correctionPackets,
                        finalFix = fix,
                        simulated = if (_isSimulated.value) 1 else 0
                    )
                )
            }
        }
        sessionStartTs = null
    }
}

// Extension dönüşümleri
private fun NtripProfileEntity.toUi() = NtripProfileUi(
    id = id,
    name = name,
    host = host,
    port = port,
    mountPoint = mountPoint,
    username = username,
    password = password,
    autoConnect = autoConnect
)
