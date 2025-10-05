package com.example.tugis3.gnss.model

/** Basit GNSS gözlem veri modeli (genişletilebilir) */
data class GnssObservation(
    val epochMillis: Long,
    val latDeg: Double?,
    val lonDeg: Double?,
    val ellipsoidalHeight: Double?,
    val fixType: FixType = FixType.SINGLE,
    val satellitesInUse: Int = 0,
    val satellitesVisible: Int = 0,
    val hrms: Double? = null,
    val vrms: Double? = null,
    val pdop: Double? = null,
    val hdop: Double? = null,
    val vdop: Double? = null,
    val rawNmea: String? = null
)

enum class FixType(val displayName: String, val accuracyLevel: Int) {
    NO_FIX("Fix Yok", 0),
    SINGLE("Tekil", 1),
    DGPS("DGPS", 2),
    RTK_FLOAT("RTK Float", 3),
    RTK_FIX("RTK Fix", 4),
    PPP("PPP", 3),
    MANUAL("Manuel", 1);

    fun isRtk(): Boolean = this == RTK_FLOAT || this == RTK_FIX
    fun isDifferential(): Boolean = accuracyLevel >= 2
}
