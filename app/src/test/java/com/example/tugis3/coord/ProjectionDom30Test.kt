package com.example.tugis3.coord

import com.example.tugis3.coord.transform.ProjectionEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionDom30Test {
    private val a = 6378137.0
    private val invF = 298.257222101 // GRS80
    private val cm = 30.0 // DOM 30
    private val lat0 = 0.0
    private val k0 = 1.0
    private val fe = 500000.0
    private val fn = 0.0

    private val tm = ProjectionEngine.createTransverseMercator(
        semiMajor = a,
        invFlattening = invF,
        centralMeridianDeg = cm,
        latOriginDeg = lat0,
        scaleFactor = k0,
        falseE = fe,
        falseN = fn
    )

    private fun assertNearUsak(lat: Double, lon: Double) {
        // Sivaslı/Akarca ~ lat ~ 38.4..38.7, lon ~ 29.3..29.9 kabaca
        assertTrue("lat out of TR local bounds: $lat", lat in 38.0..39.0)
        assertTrue("lon out of TR local bounds: $lon", lon in 29.0..30.2)
    }

    @Test
    fun test_sample_points_dom30_inverse() {
        val samples = listOf(
            // id, easting, northing
            Triple("18", 459514.040, 4271758.820),
            Triple("17", 459507.910, 4271775.090),
            Triple("11", 459460.520, 4271905.780),
            Triple("21", 459418.670, 4271726.300)
        )
        samples.forEach { (id, e, n) ->
            val (lat, lon) = tm.inverse(e, n)
            assertNearUsak(lat, lon)
        }
    }
}

