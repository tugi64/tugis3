package com.example.tugis3.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

/**
 * GNSS Bluetooth Bağlantı Yöneticisi
 *
 * South, Topcon, Trimble, Kolida gibi GNSS alıcılarına Bluetooth SPP üzerinden bağlanır.
 * NMEA 0183 verilerini okur ve listener'a iletir.
 *
 * Desteklenen UUID'ler:
 * - SPP (Serial Port Profile): 00001101-0000-1000-8000-00805F9B34FB
 * - Custom GNSS UUID: 00001101-0000-1000-8000-00805F9B34FB (SPP ile aynı, yaygın kullanım)
 */
class BluetoothGnssManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothGnssManager"

        // SPP - Serial Port Profile UUID (En yaygın)
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Alternatif UUID'ler (bazı cihazlar farklı UUID kullanabilir)
        private val ALTERNATE_UUIDS = listOf(
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), // SPP (tekrar dene)
            UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB"), // Audio Source
            UUID.fromString("0000110B-0000-1000-8000-00805F9B34FB"), // Audio Sink
            UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")  // OBD-II (bazı cihazlar kullanabilir)
        )
    }

    // BluetoothAdapter'a erişim
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private var bluetoothSocket: BluetoothSocket? = null
    private var readerThread: Thread? = null
    private var isConnected = false
    private var nmeaListener: NmeaDataListener? = null

    // Input/Output Stream'lere public erişim
    val inputStream get() = bluetoothSocket?.inputStream
    val outputStream get() = bluetoothSocket?.outputStream

    interface NmeaDataListener {
        fun onNmeaReceived(nmea: String)
        fun onConnectionLost()
    }

    fun setNmeaDataListener(listener: NmeaDataListener) {
        this.nmeaListener = listener
    }

    /**
     * GNSS cihazına bağlan
     * Birden fazla UUID deneyerek en uyumlu bağlantıyı kurar
     */
    @SuppressLint("MissingPermission")
    @Throws(IOException::class, SecurityException::class)
    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Bağlantı deneniyor: ${device.name ?: "İsimsiz"} [${device.address}]")

        // Önceki bağlantıyı kapat
        disconnect()

        var lastException: IOException? = null
        var attemptCount = 0

        // Önce SPP UUID'yi dene (en yaygın)
        try {
            attemptCount++
            Log.d(TAG, "Deneme #$attemptCount: SPP UUID ile bağlanılıyor...")

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()

            if (bluetoothSocket?.isConnected == true) {
                Log.i(TAG, "✅ SPP UUID ile bağlantı başarılı!")
                startReadingNmea()
                return
            }
        } catch (e: IOException) {
            Log.w(TAG, "SPP UUID başarısız: ${e.message}")
            lastException = e
            try {
                bluetoothSocket?.close()
            } catch (closeEx: IOException) {
                // Ignore
            }
        }

        // SPP başarısız olduysa alternatif UUID'leri dene
        for (uuid in ALTERNATE_UUIDS) {
            try {
                attemptCount++
                Log.d(TAG, "Deneme #$attemptCount: UUID $uuid ile bağlanılıyor...")

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()

                if (bluetoothSocket?.isConnected == true) {
                    Log.i(TAG, "✅ UUID $uuid ile bağlantı başarılı!")
                    startReadingNmea()
                    return
                }
            } catch (e: IOException) {
                Log.w(TAG, "UUID $uuid başarısız: ${e.message}")
                lastException = e
                try {
                    bluetoothSocket?.close()
                } catch (closeEx: IOException) {
                    // Ignore
                }
            }
        }

        // Hiçbir UUID işe yaramadıysa refleksyon ile fallback dene (Galaxy cihazlar için)
        try {
            attemptCount++
            Log.d(TAG, "Deneme #$attemptCount: Fallback yöntemi (reflection) deneniyor...")

            // Reflection ile insecure socket oluştur (bazı cihazlarda çalışır)
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            bluetoothSocket = method.invoke(device, 1) as BluetoothSocket
            bluetoothSocket?.connect()

            if (bluetoothSocket?.isConnected == true) {
                Log.i(TAG, "✅ Fallback yöntemi ile bağlantı başarılı!")
                startReadingNmea()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback yöntemi başarısız: ${e.message}")
            lastException = e as? IOException ?: IOException("Reflection failed: ${e.message}")
        }

        // Tüm yöntemler başarısız
        disconnect()
        Log.e(TAG, "❌ Bağlantı başarısız - $attemptCount deneme yapıldı")
        throw lastException ?: IOException("Bluetooth bağlantısı kurulamadı - Tüm UUID'ler denendi")
    }

    /**
     * NMEA verilerini okumaya başla
     */
    private fun startReadingNmea() {
        isConnected = true
        Log.d(TAG, "NMEA okuma başlatılıyor...")

        readerThread = Thread {
            try {
                val inputStream = bluetoothSocket?.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))

                Log.i(TAG, "📡 NMEA dinleme aktif")

                while (isConnected && !Thread.currentThread().isInterrupted) {
                    try {
                        val line = reader.readLine()

                        if (line != null) {
                            // NMEA cümleleri '$' ile başlar
                            if (line.startsWith("$")) {
                                Log.v(TAG, "NMEA: $line")
                                nmeaListener?.onNmeaReceived(line)
                            } else {
                                // NMEA değil ama debug için logla
                                Log.v(TAG, "Non-NMEA: $line")
                            }
                        } else {
                            // Stream sonu - bağlantı kesildi
                            Log.w(TAG, "Stream sonu - bağlantı kesildi")
                            break
                        }
                    } catch (e: IOException) {
                        if (isConnected) {
                            Log.e(TAG, "NMEA okuma hatası: ${e.message}")
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "NMEA stream açılamadı: ${e.message}")
            } finally {
                if (isConnected) {
                    Log.w(TAG, "NMEA okuma thread'i sonlandı - bağlantı kesildi")
                    isConnected = false
                    nmeaListener?.onConnectionLost()
                }
            }
        }.apply {
            name = "NMEA-Reader"
            start()
        }
    }

    /**
     * Bağlantıyı kes
     */
    fun disconnect() {
        Log.d(TAG, "Bağlantı kesiliyor...")
        isConnected = false

        // Thread'i durdur
        readerThread?.interrupt()
        readerThread = null

        // Socket'i kapat
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.i(TAG, "✅ Bağlantı kesildi")
        } catch (e: IOException) {
            Log.e(TAG, "Socket kapatma hatası: ${e.message}")
        }
    }

    /**
     * Bağlı mı kontrol et
     */
    fun isConnected(): Boolean = isConnected && bluetoothSocket?.isConnected == true

    /**
     * Bağlı cihazın bilgilerini al
     */
    @SuppressLint("MissingPermission")
    fun getConnectedDeviceInfo(): String? {
        return try {
            bluetoothSocket?.remoteDevice?.let { device ->
                "${device.name ?: "İsimsiz"} [${device.address}]"
            }
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Bluetooth cihaz taramasını başlat
     */
    @SuppressLint("MissingPermission")
    @Throws(SecurityException::class)
    fun startDiscovery(): Boolean {
        return try {
            bluetoothAdapter?.startDiscovery() ?: false
        } catch (e: SecurityException) {
            Log.e(TAG, "Discovery başlatma izin hatası: ${e.message}")
            throw e
        }
    }

    /**
     * Bluetooth cihaz taramasını durdur
     */
    @SuppressLint("MissingPermission")
    @Throws(SecurityException::class)
    fun stopDiscovery(): Boolean {
        return try {
            bluetoothAdapter?.cancelDiscovery() ?: false
        } catch (e: SecurityException) {
            Log.e(TAG, "Discovery durdurma izin hatası: ${e.message}")
            throw e
        }
    }

    /**
     * Mevcut veriyi oku (non-blocking)
     * Java uyumluluğu için
     * @return Okunan byte sayısı
     */
    @Throws(IOException::class)
    fun readAvailable(buffer: ByteArray): Int {
        val stream = inputStream ?: throw IOException("Bağlantı yok")
        val available = stream.available()
        if (available <= 0) return 0

        val toRead = minOf(available, buffer.size)
        return stream.read(buffer, 0, toRead)
    }

    /**
     * Veri gönder
     * Java uyumluluğu için
     */
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        val stream = outputStream ?: throw IOException("Bağlantı yok")
        stream.write(data)
        stream.flush()
        Log.d(TAG, "Veri gönderildi: ${data.size} byte")
    }

    /**
     * String veri gönder (convenience method)
     */
    @Throws(IOException::class)
    fun write(data: String) {
        write(data.toByteArray())
    }
}

