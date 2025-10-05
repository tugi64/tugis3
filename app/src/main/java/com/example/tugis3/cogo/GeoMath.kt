package com.example.tugis3.cogo

import kotlin.math.*

object GeoMath {
    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    fun dmsFormat(deg: Double, secondsPrecision: Int = 2): String {
        val sign = if (deg < 0) -1 else 1
        var a = abs(deg)
        val d = floor(a)
        a = (a - d) * 60.0
        val m = floor(a)
        val s = (a - m) * 60.0
        return String.format("%s%02.0f°%02.0f'%0${2 + secondsPrecision + if (secondsPrecision>0) 1 else 0}.${secondsPrecision}f\"",
            if (sign<0) "-" else "", d, m, s)
    }

    fun azimuthDeg(n1: Double, e1: Double, n2: Double, e2: Double): Double {
        val dn = n2 - n1
        val de = e2 - e1
        var az = atan2(de, dn) * RAD_TO_DEG // North-based
        if (az < 0) az += 360.0
        return az
    }

    fun horizontalDistance(n1: Double, e1: Double, n2: Double, e2: Double): Double {
        val dn = n2 - n1
        val de = e2 - e1
        return hypot(dn, de)
    }

    fun slopeDistance(hDist: Double, dh: Double): Double = hypot(hDist, dh)

    fun slopeRatio(hDist: Double, dh: Double): String {
        if (abs(dh) < 1e-9 || hDist < 1e-9) return "-"
        val ratio = hDist / abs(dh)
        return String.format(java.util.Locale.US, "1:%.1f", ratio)
    }

    // İleri projeksiyon: başlangıç N/E + mesafe + azimut -> hedef N/E
    fun forwardNE(n: Double, e: Double, distance: Double, azimuthDeg: Double): Pair<Double, Double> {
        val rad = azimuthDeg * DEG_TO_RAD
        val dn = distance * cos(rad)
        val de = distance * sin(rad)
        return Pair(n + dn, e + de)
    }

    // Üç nokta arasındaki (A-B-C) iç açı (B tepe) hesapla (0-180°). Degenerate durumda null
    fun angleAt(nA: Double, eA: Double, nB: Double, eB: Double, nC: Double, eC: Double): Double? {
        val v1n = nA - nB; val v1e = eA - eB
        val v2n = nC - nB; val v2e = eC - eB
        val len1 = hypot(v1n, v1e); val len2 = hypot(v2n, v2e)
        if (len1 < 1e-9 || len2 < 1e-9) return null
        val dot = v1n * v2n + v1e * v2e
        val cosAng = (dot / (len1 * len2)).coerceIn(-1.0, 1.0)
        val ang = acos(cosAng) * RAD_TO_DEG
        return ang
    }
}
