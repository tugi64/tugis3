package com.example.tugis3.ntrip

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import android.util.Base64
import kotlin.time.Duration.Companion.seconds

/**
 * NTRIP istemcisi (Networked Transport of RTCM via Internet Protocol).
 * RTCM düzeltmelerini caster'dan alır ve RTCM veya NMEA olarak yayınlar.
 * Simülasyon modunda sahte NMEA verileri üretir.
 */
interface NtripClient {
    data class Config(
        val host: String,
        val port: Int = 2101,
        val mountPoint: String,
        val username: String? = null,
        val password: String? = null,
        val simulate: Boolean = true, // true ise sahte veri üretir
        val connectTimeoutMs: Int = 15000,
        val reconnectIntervalMs: Int = 10000,
        val ggaIntervalSec: Int = 10 // gerçek modda periyodik GGA uplink
    )

    sealed interface Event {
        object Connecting : Event
        object Connected : Event
        data class Data(val bytes: ByteArray) : Event {
            // ByteArray için özel equals/hashCode
            override fun equals(other: Any?): Boolean =
                other is Data && bytes.contentEquals(other.bytes)

            override fun hashCode(): Int = bytes.contentHashCode()
        }
        data class Error(val message: String) : Event
        object Stopped : Event
    }

    val events: SharedFlow<Event>
    fun start(cfg: Config)
    fun stop()
    /** GNSS pozisyonu güncelle – gerçek bağlantıda periyodik GGA üretilecek */
    fun updatePosition(latDeg: Double, lonDeg: Double, ellHeight: Double?, fixQuality: Int, satellites: Int)

    // Veri analizi için yardımcı uzantılar
    companion object {
        // İçeriğin RTCM paketi olup olmadığını belirle
        fun isRtcmMessage(bytes: ByteArray): Boolean {
            // RTCM 3.x formatı: ilk byte 0xD3, ikinci ve üçüncü byteler genellikle 0x00
            return bytes.size >= 3 && (bytes[0].toInt() and 0xFF) == 0xD3
        }

        // İçeriğin NMEA satırı olup olmadığını belirle
        fun isNmeaSentence(bytes: ByteArray): Boolean {
            val str = String(bytes, StandardCharsets.US_ASCII)
            return str.trim().startsWith("$") && str.contains("*")
        }
    }
}

class NtripClientImpl : NtripClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<NtripClient.Event>(extraBufferCapacity = 64)
    override val events: SharedFlow<NtripClient.Event> = _events.asSharedFlow()

    @Volatile
    private var job: Job? = null
    private var isRunning = false

    @Volatile private var lastLat: Double? = null
    @Volatile private var lastLon: Double? = null
    @Volatile private var lastHeight: Double? = null
    @Volatile private var lastFixQ: Int = 0
    @Volatile private var lastSats: Int = 0

    @Synchronized
    override fun start(cfg: NtripClient.Config) {
        stop()
        isRunning = true
        job = scope.launch {
            if (cfg.simulate) {
                // Simülasyon modu: periyodik sahte NMEA
                simulateLoop()
            } else {
                // Gerçek NTRIP bağlantısı, kesintisiz yeniden deneme
                var retryCount = 0
                while (isActive && isRunning) {
                    _events.tryEmit(NtripClient.Event.Connecting)
                    try {
                        realLoop(cfg)
                    } catch (e: Exception) {
                        val msg = if (retryCount == 0) e.message ?: "Bağlantı hatası"
                                 else "Yeniden deneniyor (${retryCount+1})..."
                        _events.tryEmit(NtripClient.Event.Error(msg))

                        // Artan bekleme zamanları ile yeniden deneme
                        val waitTime = minOf(10000L + (retryCount * 5000), 60000L)
                        delay(waitTime)
                        retryCount++
                    }

                    // Kullanıcı kapatmadıysa ve hala aktifse yeniden dene
                    if (!isRunning || !isActive) break
                    delay(cfg.reconnectIntervalMs.toLong())
                }
                _events.tryEmit(NtripClient.Event.Stopped)
            }
        }
    }

    @Synchronized
    override fun stop() {
        isRunning = false
        job?.cancel()
        job = null
        _events.tryEmit(NtripClient.Event.Stopped)
    }

    override fun updatePosition(latDeg: Double, lonDeg: Double, ellHeight: Double?, fixQuality: Int, satellites: Int) {
        lastLat = latDeg; lastLon = lonDeg; lastHeight = ellHeight; lastFixQ = fixQuality; lastSats = satellites
    }

    private suspend fun simulateLoop() {
        _events.tryEmit(NtripClient.Event.Connected)
        var counter = 0
        while (scope.isActive && isRunning) {
            // Basit geçerli checksum üretimi
            val ggaCore = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,,"
            val gga = "${'$'}$ggaCore*" + checksum(ggaCore)
            _events.tryEmit(NtripClient.Event.Data((gga + "\r\n").toByteArray()))
            counter++
            if (counter % 5 == 0) {
                val gsaCore = "GPGSA,A,3,04,05,09,12,24,29,31,02,,,,1.8,1.0,1.5"
                val gsa = "${'$'}$gsaCore*" + checksum(gsaCore)
                _events.tryEmit(NtripClient.Event.Data((gsa + "\r\n").toByteArray()))
            }
            if (counter % 7 == 0) {
                val gsvCore = "GPGSV,2,1,08,01,40,083,41,02,17,308,42,03,13,172,43,04,29,120,45"
                val gsv = "${'$'}$gsvCore*" + checksum(gsvCore)
                _events.tryEmit(NtripClient.Event.Data((gsv + "\r\n").toByteArray()))
            }

            // RTK fix tip simülasyonu (her 20 saniyede bir)
            if (counter % 20 == 0) {
                val rtcmBytes = ByteArray(100) { if (it == 0) 0xD3.toByte() else (it % 256).toByte() }
                _events.tryEmit(NtripClient.Event.Data(rtcmBytes))
                val rtk = "${'$'}GPGGA,123519,4807.038,N,01131.000,E,4,12,0.6,545.4,M,46.9,M,,*7C"
                _events.tryEmit(NtripClient.Event.Data((rtk + "\r\n").toByteArray()))
            }

            delay(1.seconds)
        }
    }

    private fun realLoop(cfg: NtripClient.Config) {
        // Soket oluştur ve bağlan (timeout ile)
        val socket = Socket()
        try {
            socket.soTimeout = cfg.connectTimeoutMs
            socket.connect(InetSocketAddress(cfg.host, cfg.port), cfg.connectTimeoutMs)

            val out: OutputStream = socket.getOutputStream()
            val mount = if (cfg.mountPoint.startsWith("/")) cfg.mountPoint.drop(1) else cfg.mountPoint

            // Basic auth ekle (android.util.Base64)
            val auth = if (!cfg.username.isNullOrBlank()) {
                val authStr = if (cfg.password.isNullOrBlank()) "${cfg.username}:" else "${cfg.username}:${cfg.password}"
                val token = Base64.encodeToString(authStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "Authorization: Basic $token\r\n"
            } else ""

            // HTTP GET isteği oluştur
            val request = buildString {
                append("GET /$mount HTTP/1.0\r\n")
                append("User-Agent: tugis3-ntrip/0.2\r\n")
                append("Accept: */*\r\n")
                append(auth)
                append("Connection: close\r\n\r\n")
            }

            // İsteği gönder
            out.write(request.toByteArray())
            out.flush()

            // Yanıt başlığını oku
            val bis = BufferedInputStream(socket.getInputStream())
            val headerBuffer = StringBuilder()
            var crlfCount = 0
            while (true) {
                val b = bis.read()
                if (b == -1) break
                val c = b.toChar()
                headerBuffer.append(c)
                if (c == '\r' || c == '\n') {
                    crlfCount++
                    if (crlfCount == 4) break
                } else {
                    crlfCount = 0
                }
                if (headerBuffer.length > 2000) throw Exception("HTTP yanıt başlığı çok uzun veya bozuk")
            }
            val header = headerBuffer.toString()

            // HTTP başlığını kontrol et
            if (!header.startsWith("HTTP/1.") || !header.contains(" 200 ")) {
                val errorMsg = when {
                    header.contains("401") -> "Kimlik doğrulama hatası (401)"
                    header.contains("404") -> "Mount point bulunamadı (404)"
                    else -> "Sunucu hatası: " + header.split("\r\n").firstOrNull()
                }
                throw Exception(errorMsg)
            }

            _events.tryEmit(NtripClient.Event.Connected)

            // GGA uplink job
            val ggaJob = scope.launch {
                while (isActive && isRunning) {
                    if (!cfg.simulate) {
                        val lat = lastLat; val lon = lastLon
                        if (lat != null && lon != null) {
                            val gga = buildGgaSentence(lat, lon, lastHeight, lastFixQ, lastSats)
                            runCatching { out.write((gga + "\r\n").toByteArray()); out.flush() }.getOrElse { }
                        }
                    }
                    delay((cfg.ggaIntervalSec * 1000).toLong())
                }
            }

            val buffer = ByteArray(8192)
            while (scope.isActive && isRunning) {
                val n = bis.read(buffer)
                if (n <= 0) break

                _events.tryEmit(NtripClient.Event.Data(buffer.copyOfRange(0, n)))

                // Veri tamamiyle okundu mu? (başlık mesajı bitimini kontrol et)
                if (header.contains("Content-Length:")) {
                    break  // Tek blok veri - okuduktan sonra çık
                }
            }
            ggaJob.cancel()
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildGgaSentence(latDeg: Double, lonDeg: Double, h: Double?, fixQ: Int, sats: Int): String {
        fun toDmm(value: Double, lat: Boolean): Pair<String, String> {
            val absV = kotlin.math.abs(value)
            val deg = absV.toInt()
            val min = (absV - deg) * 60.0
            val dFmt = if (lat) "%02d" else "%03d"
            val head = dFmt.format(deg)
            val body = "%07.4f".format(min)
            val hemi = if (lat) if (value >= 0) "N" else "S" else if (value >= 0) "E" else "W"
            return (head + body) to hemi
        }
        val (latStr, latH) = toDmm(latDeg, true)
        val (lonStr, lonH) = toDmm(lonDeg, false)
        val ts = java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).run { "%02d%02d%02d".format(hour, minute, second) }
        val alt = h ?: 0.0
        val core = "GPGGA,$ts,$latStr,$latH,$lonStr,$lonH,$fixQ,%02d,1.0,%.1f,M,0.0,M,,".format(sats.coerceIn(0,99), alt)
        val cs = checksum(core)
        return "$$core*$cs"
    }

    private fun checksum(body: String): String {
        var cs = 0
        for (c in body) cs = cs xor c.code
        return cs.toString(16).uppercase().padStart(2, '0')
    }
}
