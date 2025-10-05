package com.example.tugis3.coord.transform

import kotlin.math.*
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Basit UTM ileri/ters round-trip testi.
 * Farkın (haversine) < 1 m olması beklenir.
 */
class UtmTransformerTest {

    private val a = 6378137.0               // WGS84 semi-major
    private val invF = 298.257223563        // WGS84 inverse flattening

    private fun zoneForLon(lon: Double): Int = floor((lon + 180.0) / 6.0).toInt() + 1

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371008.8 // ortalama yarıçap (metre)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val sLat1 = sin(Math.toRadians(lat1))
        val sLat2 = sin(Math.toRadians(lat2))
        val aH = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * sLat1 * sLat2
        val c = 2 * atan2(sqrt(aH), sqrt(1 - aH))
        return r * c
    }

    private fun roundTrip(lat: Double, lon: Double): Double {
        val zone = zoneForLon(lon)
        val northern = lat >= 0
        val transformer = UtmTransformer(zone, northern, a, invF)
        val (x, y) = transformer.forward(lat, lon)
        val (ilat, ilon) = transformer.inverse(x, y)
        return haversine(lat, lon, ilat, ilon)
    }

    @Test
    fun testRoundTrip_Ankara() {
        val d = roundTrip(39.92077, 32.85411) // Ankara civarı
        assertTrue("UTM round-trip mesafe > 1 m: $d", d < 1.0)
    }

    @Test
    fun testRoundTrip_Equator() {
        val d = roundTrip(0.1234, 3.4567)
        assertTrue("UTM round-trip mesafe > 1 m: $d", d < 1.0)
    }

    @Test
    fun testRoundTrip_SouthernHemisphere() {
        val d = roundTrip(-15.789, -55.123) // Güney Amerika
        assertTrue("UTM round-trip mesafe > 1 m: $d", d < 1.0)
    }
}

