package com.example.tugis3.core.cad.geom

import com.example.tugis3.core.cad.codec.CadCodec
import com.example.tugis3.core.cad.export.CadExportUtil
import com.example.tugis3.core.cad.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class GeometryAndCodecTest {

    private fun approx(a: Double, b: Double, eps: Double = 1e-9) {
        assertTrue("$a != $b (±$eps)", kotlin.math.abs(a - b) <= eps)
    }

    @Test
    fun testPolylineLength() {
        val pts = listOf(Vec2(0.0,0.0), Vec2(3.0,4.0)) // distance 5
        val len = polylineLength(pts)
        approx(5.0, len, 1e-9)
    }

    @Test
    fun testPolygonAreaWithHole() {
        val outer = listOf(
            Vec2(0.0,0.0), Vec2(10.0,0.0), Vec2(10.0,10.0), Vec2(0.0,10.0)
        )
        val hole = listOf(
            Vec2(2.0,2.0), Vec2(8.0,2.0), Vec2(8.0,8.0), Vec2(2.0,8.0)
        )
        val areaOuter = polygonArea(outer)
        val areaHole = polygonArea(hole)
        approx(100.0, areaOuter, 1e-9)
        approx(36.0, areaHole, 1e-9)
        val pg = CadPolygon(rings = listOf(outer, hole))
        approx(64.0, polygonAreaRings(pg.rings), 1e-9)
    }

    @Test
    fun testArcLength() {
        val r = 5.0
        val start = 30.0
        val end = 90.0
        val len = arcLength(r, start, end)
        approx(r * PI / 3.0, len, 1e-9) // 60 deg => PI/3 radians
    }

    @Test
    fun testCircleAreaCircumference() {
        val r = 2.5
        approx(PI * r * r, circleArea(r), 1e-12)
        approx(2 * PI * r, circleCircumference(r), 1e-12)
    }

    @Test
    fun testCodecRoundTripBasic() {
        val entities: List<CadEntity> = listOf(
            CadLine(Vec2(0.0,0.0), Vec2(1.0,1.0), layer = "L1", colorIndex = 2),
            CadPolyline(listOf(Vec2(0.0,0.0), Vec2(1.0,0.0), Vec2(1.0,1.0)), isClosed = true, layer = "L2", colorIndex = 3),
            CadCircle(Vec2(5.0,5.0), radius = 2.0, layer = "CIRC"),
            CadArc(Vec2(0.0,0.0), radius = 3.0, startAngleDeg = 10.0, endAngleDeg = 80.0, layer = "ARC"),
            CadText(Vec2(2.0,2.0), height = 1.5, text = "HELLO|PIPE", layer = "TXT", colorIndex = 5)
        )
        entities.forEach { e ->
            val enc = CadCodec.encode(e)
            val dec = CadCodec.decode(enc.first, enc.second, e.layer, e.colorIndex)
            assertNotNull("Decode null for ${e::class.simpleName}", dec)
            when(e) {
                is CadLine -> {
                    val d = dec as CadLine
                    approx(e.start.x, d.start.x); approx(e.end.y, d.end.y)
                }
                is CadPolyline -> {
                    val d = dec as CadPolyline
                    assertEquals(e.points.size, d.points.size)
                    assertEquals(e.isClosed, d.isClosed)
                }
                is CadCircle -> {
                    val d = dec as CadCircle
                    approx(e.radius, d.radius)
                }
                is CadArc -> {
                    val d = dec as CadArc
                    approx(e.startAngleDeg, d.startAngleDeg)
                    approx(e.endAngleDeg, d.endAngleDeg)
                }
                is CadText -> {
                    val d = dec as CadText
                    assertEquals(e.text, d.text)
                }
                else -> {}
            }
        }
    }

    @Test
    fun testCodecPolygonRoundTrip() {
        val outer = listOf(
            Vec2(0.0,0.0), Vec2(4.0,0.0), Vec2(4.0,4.0), Vec2(0.0,4.0)
        )
        val hole = listOf(
            Vec2(1.0,1.0), Vec2(2.0,1.0), Vec2(2.0,2.0), Vec2(1.0,2.0)
        )
        val pg = CadPolygon(rings = listOf(outer, hole), layer = "POLY", colorIndex = 7)
        val enc = CadCodec.encode(pg)
        assertEquals("PG", enc.first)
        val dec = CadCodec.decode(enc.first, enc.second, pg.layer, pg.colorIndex)
        assertTrue(dec is CadPolygon)
        dec as CadPolygon
        assertEquals(2, dec.rings.size)
        assertEquals(outer.size, dec.rings.first().size)
        approx(polygonAreaRings(pg.rings), polygonAreaRings(dec.rings), 1e-9)
    }

    @Test
    fun testPolygonPerimeterOuterVsRings() {
        val outer = listOf(
            Vec2(0.0,0.0), Vec2(6.0,0.0), Vec2(6.0,4.0), Vec2(0.0,4.0)
        ) // P = 20
        val hole = listOf(
            Vec2(1.0,1.0), Vec2(2.0,1.0), Vec2(2.0,2.0), Vec2(1.0,2.0)
        ) // P = 4
        val rings = listOf(outer, hole)
        val total = polygonPerimeterRings(rings)
        val outerOnly = polygonOuterPerimeter(rings)
        approx(20.0, outerOnly, 1e-9)
        approx(24.0, total, 1e-9)
        assertTrue("Total perimeter outer'dan büyük olmalı", total > outerOnly)
    }

    @Test
    fun testGeoJsonExportBasicCounts() {
        val entities: List<CadEntity> = listOf(
            CadLine(Vec2(0.0,0.0), Vec2(1.0,0.0), layer = "L1"),
            CadCircle(Vec2(2.0,2.0), 1.0, layer = "L2"),
            CadText(Vec2(5.0,5.0), 2.0, "TXT", layer = "T1")
        )
        val json = CadExportUtil.toGeoJson(entities, CadExportUtil.Options(circleSegments = 16))
        assertTrue(json.contains("\"FeatureCollection\""))
        // 3 entity -> 3 feature
        val featureCount = Regex("\"type\":\"Feature\"").findAll(json).count()
        assertEquals(3L, featureCount)
        // Circle -> Polygon
        assertTrue(json.contains("\"type\":\"Polygon\""))
    }

    @Test
    fun testGeoJsonCircleArcSegmentation() {
        val circleSegments = 12
        val circle = CadCircle(Vec2(0.0,0.0), 5.0, layer = "C")
        val arc = CadArc(Vec2(10.0,0.0), 3.0, 0.0, 90.0, layer = "A") // 90° sweep
        val json = CadExportUtil.toGeoJson(listOf(circle, arc), CadExportUtil.Options(circleSegments = circleSegments, arcSegmentAngleDeg = 30.0))
        // Polygon coordinate count (circle) ~ segments + 1 (closure)
        // Extract first Polygon coord array
        val polyRegex = Regex("\"type\":\"Polygon\",\"coordinates\":(\\[[^}]*)")
        val match = polyRegex.find(json)
        assertNotNull("Polygon bulunamadı", match)
        match ?: return
        val coordsPart = match.groupValues[1]
        val pairCount = Regex("\\[[-0-9]").findAll(coordsPart).count() - 1
        assertTrue("Beklenen segments civarı koordinat (>= circleSegments)", pairCount >= circleSegments)
        // Arc -> LineString ve nokta sayısı sweep/segmentAngle ~ 3 -> steps 3 + 1 = 4
        val lineRegex = Regex("\"type\":\"LineString\",\"coordinates\":(\\[[^}]*)")
        val lineMatch = lineRegex.find(json)
        assertNotNull("Arc LineString bulunamadı", lineMatch)
        lineMatch ?: return
        val lineCoords = lineMatch.groupValues[1]
        val linePairs = Regex("\\[[-0-9]").findAll(lineCoords).count()
        assertEquals("Arc beklenen nokta sayısı", 4, linePairs)
    }
}
