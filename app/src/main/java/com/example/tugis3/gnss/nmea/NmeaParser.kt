package com.example.tugis3.gnss.nmea

import com.example.tugis3.gnss.model.FixType
import kotlin.math.abs

/**
 * Basit / hafif NMEA v2.x çözümleyici – yalnızca GGA ve RMC satırlarını işler.
 *
 * Üretim için: checksum doğrulama, daha fazla cümle (GSA / GSV), time / date birleşimi vb. eklenebilir.
 */
class NmeaParser {
    data class Parsed(
        val latDeg: Double? = null,
        val lonDeg: Double? = null,
        val heightEllipsoidal: Double? = null,
        val satellitesInUse: Int? = null,
        val satellitesVisible: Int? = null,
        val hdop: Double? = null,
        val vdop: Double? = null,
        val pdop: Double? = null,
        val fixType: FixType? = null,
        val raw: String
    )

    fun parse(line: String): Parsed? {
        if (!line.startsWith('$') || line.length < 10) return null
        // Checksum doğrulama (opsiyonel ama etkin)
        if (!validateChecksum(line)) return null
        val starIdx = line.indexOf('*')
        val body = if (starIdx > 0) line.substring(1, starIdx) else line.substring(1)
        val fields = body.split(',')
        if (fields.isEmpty()) return null
        return when (fields[0]) {
            "GPGGA", "GNGGA", "GAGGA" -> parseGGA(fields, line)
            "GPRMC", "GNRMC", "GARMC" -> parseRMC(fields, line)
            "GPGSA", "GNGSA", "GAGSA" -> parseGSA(fields, line)
            "GPGSV", "GNGSV", "GAGSV" -> parseGSV(fields, line)
            else -> null
        }
    }

    private fun parseGGA(f: List<String>, raw: String): Parsed? {
        // GGA formatı: $..GGA,utc,lat,NS,lon,EW,fix,sats,hdop,alt,M,geoid,M,...
        if (f.size < 10) return null
        val fixQuality = f.getOrNull(6)?.toIntOrNull() ?: 0
        val satCount = f.getOrNull(7)?.toIntOrNull()
        val hdop = f.getOrNull(8)?.toDoubleOrNull()
        val alt = f.getOrNull(9)?.toDoubleOrNull()
        val fix = mapFixQuality(fixQuality)
        val lat = dmToDeg(f.getOrNull(2), f.getOrNull(3))
        val lon = dmToDeg(f.getOrNull(4), f.getOrNull(5))
        return Parsed(
            latDeg = lat,
            lonDeg = lon,
            heightEllipsoidal = alt,
            satellitesInUse = satCount,
            hdop = hdop,
            fixType = fix,
            raw = raw
        )
    }

    private fun parseRMC(f: List<String>, raw: String): Parsed? {
        // RMC formatı: $..RMC,utc,status,lat,NS,lon,EW,sog,cog,date,...
        if (f.size < 10) return null
        val status = f.getOrNull(2)
        if (status != "A") { // A=Active, V=Void
            return Parsed(raw = raw, fixType = FixType.NO_FIX)
        }
        val lat = dmToDeg(f.getOrNull(3), f.getOrNull(4))
        val lon = dmToDeg(f.getOrNull(5), f.getOrNull(6))
        return Parsed(
            latDeg = lat,
            lonDeg = lon,
            raw = raw,
            // RMC kendi başına fix tipini ayırt etmez – SINGLE varsay.
            fixType = FixType.SINGLE
        )
    }

    private fun parseGSA(f: List<String>, raw: String): Parsed? {
        // $..GSA,Mode1,Mode2,Sat1,...,Sat12,PDOP,HDOP,VDOP*CS
        if (f.size < 17) return null
        val pdop = f.getOrNull(15)?.toDoubleOrNull()
        val hdop = f.getOrNull(16)?.toDoubleOrNull()
        val vdField = f.getOrNull(17)
        val vdop = vdField?.substringBefore('*')?.toDoubleOrNull()
        // Kullanılan uydu sayısı: sat alanlarından boş olmayanların sayısı
        val satInUse = f.subList(3, 15).count { it.isNotBlank() }
        // Mode2 -> 1: no fix, 2: 2D, 3: 3D (basit eşleme)
        val mode2 = f.getOrNull(2)
        val fixType = when (mode2) {
            "1" -> FixType.NO_FIX
            "2" -> FixType.SINGLE
            "3" -> FixType.SINGLE
            else -> null
        }
        return Parsed(
            satellitesInUse = if (satInUse > 0) satInUse else null,
            hdop = hdop,
            vdop = vdop,
            pdop = pdop,
            fixType = fixType,
            raw = raw
        )
    }

    private fun parseGSV(f: List<String>, raw: String): Parsed? {
        // $..GSV,totalMsgs,msgIdx,totalSats, ... (her cümlede 4 uyduya kadar)
        if (f.size < 4) return null
        val total = f.getOrNull(3)?.toIntOrNull()
        return Parsed(
            satellitesVisible = total,
            raw = raw
        )
    }

    private fun mapFixQuality(q: Int): FixType = when (q) {
        0 -> FixType.NO_FIX
        1 -> FixType.SINGLE
        2 -> FixType.DGPS
        3 -> FixType.PPP
        4 -> FixType.RTK_FIX
        5 -> FixType.RTK_FLOAT
        6 -> FixType.DGPS // Estimated (dead reckoning)
        7 -> FixType.MANUAL // Manual input mode
        8 -> FixType.DGPS // Simulator mode
        else -> FixType.SINGLE
    }

    private fun dmToDeg(dm: String?, hemi: String?): Double? {
        if (dm.isNullOrBlank() || hemi.isNullOrBlank()) return null
        val dot = dm.indexOf('.')
        if (dot < 0) return null
        // Latitude ddmm.mmmm, Longitude dddmm.mmmm
        val degLen = if (dot > 3) dot - 2 else dot - 2
        val degPart = dm.take(degLen).toIntOrNull() ?: return null
        val minPart = dm.drop(degLen).toDoubleOrNull() ?: return null
        var v = degPart + (minPart / 60.0)
        if (hemi.equals("S", true) || hemi.equals("W", true)) v = -abs(v)
        return v
    }

    private fun validateChecksum(line: String): Boolean {
        val star = line.indexOf('*')
        if (star < 0 || star + 3 > line.length) return false
        val provided = line.substring(star + 1).trim()
        val body = line.substring(1, star)
        var cs = 0
        for (c in body) cs = cs xor c.code
        val hex = cs.toString(16).uppercase().padStart(2, '0')
        return hex == provided.uppercase()
    }
}
