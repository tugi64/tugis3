package com.example.tugis3.gnss

import com.example.tugis3.gnss.model.FixType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.measureTimeMillis

/**
 * Uygulama genelinde GNSS (NMEA) konum bilgisini paylaşmak için hafif singleton repository.
 */
object GnssPositionRepository {
    data class Position(
        val lat: Double? = null,
        val lon: Double? = null,
        val alt: Double? = null,
        val hdop: Double? = null,
        val vdop: Double? = null,
        val pdop: Double? = null,
        val satsInUse: Int? = null,
        val satsVisible: Int? = null,
        val fixType: FixType? = null,
        val lastNmea: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _position = MutableStateFlow(Position())
    val position: StateFlow<Position> = _position.asStateFlow()

    fun update(
        lat: Double?, lon: Double?, alt: Double?, hdop: Double?, vdop: Double?, pdop: Double?,
        satsUse: Int?, satsVis: Int?, fix: FixType?, raw: String?
    ) {
        // Konum varsa güncelle; hdop yoksa eskisini koru vb.
        val prev = _position.value
        _position.value = prev.copy(
            lat = lat ?: prev.lat,
            lon = lon ?: prev.lon,
            alt = alt ?: prev.alt,
            hdop = hdop ?: prev.hdop,
            vdop = vdop ?: prev.vdop,
            pdop = pdop ?: prev.pdop,
            satsInUse = satsUse ?: prev.satsInUse,
            satsVisible = satsVis ?: prev.satsVisible,
            fixType = fix ?: prev.fixType,
            lastNmea = raw ?: prev.lastNmea,
            timestamp = System.currentTimeMillis()
        )
    }
}

