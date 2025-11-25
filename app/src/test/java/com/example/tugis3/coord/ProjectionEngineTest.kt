package com.example.tugis3.coord

import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.coord.transform.UtmTransformer
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class ProjectionEngineTest {

    private val a = 6378137.0
    private val invF = 298.257222101

    private fun assertApprox(a: Double, b: Double, tol: Double, msg: String = "") {
        assertEquals(msg, 0.0, abs(a - b), tol)
    }

    @Test
    fun utm_forward_inverse_roundtrip() {
        // Ankara civarı
        val lat = 39.92077
        val lon = 32.85411
        val zone = ((lon + 180.0) / 6.0).toInt() + 1
        val tf = UtmTransformer(zone, true, a, invF)
        val (e, n) = tf.forward(lat, lon)
        val (lat2, lon2) = tf.inverse(e, n)
        assertApprox(lat, lat2, 1e-8, "UTM lat roundtrip")
        assertApprox(lon, lon2, 1e-8, "UTM lon roundtrip")
    }

    @Test
    fun tm30_forward_inverse_roundtrip() {
        val lat = 39.0
        val lon = 32.0
        val tm = ProjectionEngine.createTransverseMercator(
            semiMajor = a,
            invFlattening = invF,
            centralMeridianDeg = 30.0,
            latOriginDeg = 0.0,
            scaleFactor = 1.0,
            falseE = 500000.0,
            falseN = 0.0
        )
        val (e, n) = tm.forward(lat, lon)
        val (lat2, lon2) = tm.inverse(e, n)
        assertApprox(lat, lat2, 1e-8, "TM30 lat roundtrip")
        assertApprox(lon, lon2, 1e-8, "TM30 lon roundtrip")
    }

    @Test
    fun tm30_to_utm_chain_preserves_geo() {
        val lat = 40.0
        val lon = 32.0
        val tm = ProjectionEngine.createTransverseMercator(
            semiMajor = a,
            invFlattening = invF,
            centralMeridianDeg = 30.0,
            latOriginDeg = 0.0,
            scaleFactor = 1.0,
            falseE = 500000.0,
            falseN = 0.0
        )
        val (eTm, nTm) = tm.forward(lat, lon)
        val (latFromTm, lonFromTm) = tm.inverse(eTm, nTm)
        val zone = ((lonFromTm + 180.0) / 6.0).toInt() + 1
        val utm = UtmTransformer(zone, true, a, invF)
        val (eU, nU) = utm.forward(latFromTm, lonFromTm)
        val (lat2, lon2) = utm.inverse(eU, nU)
        assertApprox(lat, lat2, 1e-8, "Chain lat")
        assertApprox(lon, lon2, 1e-8, "Chain lon")
    }
}

