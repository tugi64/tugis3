package com.example.tugis3.core.cad.export

import com.example.tugis3.core.cad.model.*
import kotlin.math.*

/**
 * CAD entity listesini GeoJSON FeatureCollection string'ine dönüştürür.
 * Circle ve Arc için segment yaklaşımı yapılır.
 */
object CadExportUtil {
    data class Options(
        val circleSegments: Int = 64,
        val arcSegmentAngleDeg: Double = 10.0 // arc için maksimum segment açısı
    )

    fun toGeoJson(entities: List<CadEntity>, options: Options = Options()): String {
        val features = entities.mapNotNull { e ->
            val geom = geometryObject(e, options) ?: return@mapNotNull null
            val props = mutableMapOf<String, Any?>().apply {
                put("layer", e.layer)
                put("type", e.type.name)
                put("colorIndex", e.colorIndex)
                when (e) {
                    is CadLine -> {}
                    is CadPoint -> {}
                    is CadPolygon -> {}
                    is CadPolyline -> {}
                    is CadText -> { put("text", e.text); put("height", e.height) }
                    is CadArc -> { put("startAngleDeg", e.startAngleDeg); put("endAngleDeg", e.endAngleDeg) }
                    is CadCircle -> { put("radius", e.radius) }
                }
            }
            feature(props, geom)
        }
        return buildString {
            append("{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n")
            features.forEachIndexed { idx, f ->
                append("    ").append(f)
                if (idx != features.lastIndex) append(',')
                append('\n')
            }
            append("  ]\n}")
        }
    }

    private fun feature(props: Map<String, Any?>, geometryJson: String): String = buildString {
        append('{')
        append("\"type\":\"Feature\",")
        append("\"properties\":{")
        var first = true
        props.forEach { (k,v) ->
            if (v == null) return@forEach
            if (!first) append(',') else first = false
            append('"').append(escape(k)).append('"').append(':')
            when(v) {
                is Number, is Boolean -> append(v.toString())
                else -> append('"').append(escape(v.toString())).append('"')
            }
        }
        append("},\"geometry\":").append(geometryJson).append('}')
    }

    private fun geometryObject(e: CadEntity, opt: Options): String? = when(e) {
        is CadLine -> lineString(listOf(e.start, e.end))
        is CadPolyline -> if (e.isClosed && e.points.size >= 3) polygon(listOf(e.points)) else lineString(e.points)
        is CadPolygon -> polygon(e.rings)
        is CadCircle -> polygon(listOf(approxCircle(e.center, e.radius, opt.circleSegments)))
        is CadArc -> lineString(approxArc(e.center, e.radius, e.startAngleDeg, e.endAngleDeg, opt.arcSegmentAngleDeg))
        is CadText -> point(e.position)
        is CadPoint -> point(e.position)
        else -> null
    }

    private fun point(p: Vec2): String = "{\"type\":\"Point\",\"coordinates\":[${p.x},${p.y}]}"
    private fun lineString(points: List<Vec2>): String {
        if (points.isEmpty()) return "{\"type\":\"LineString\",\"coordinates\":[]}"
        val coords = points.joinToString(prefix="[", postfix="]") { "[${it.x},${it.y}]" }
        return "{\"type\":\"LineString\",\"coordinates\":$coords}"
    }
    private fun polygon(rings: List<List<Vec2>>): String {
        if (rings.isEmpty()) return "{\"type\":\"Polygon\",\"coordinates\":[]}"
        val ringsJson = rings.joinToString(prefix="[", postfix="]") { ring ->
            val closed = if (ring.isNotEmpty() && (ring.first().x != ring.last().x || ring.first().y != ring.last().y)) ring + ring.first() else ring
            val c = closed.joinToString(prefix="[", postfix="]") { p -> "[${p.x},${p.y}]" }
            c
        }
        return "{\"type\":\"Polygon\",\"coordinates\":$ringsJson}"
    }

    private fun approxCircle(center: Vec2, r: Double, segments: Int): List<Vec2> {
        val seg = max(8, segments)
        val pts = ArrayList<Vec2>(seg)
        val step = 2 * Math.PI / seg
        var a = 0.0
        for (i in 0 until seg) {
            pts += Vec2(center.x + cos(a) * r, center.y + sin(a) * r)
            a += step
        }
        return pts
    }

    private fun approxArc(center: Vec2, r: Double, startDeg: Double, endDeg: Double, maxSegAngleDeg: Double): List<Vec2> {
        var sweep = endDeg - startDeg
        if (sweep < 0) sweep += 360.0
        val steps = max(2, (sweep / maxSegAngleDeg).roundToInt())
        val pts = ArrayList<Vec2>(steps + 1)
        val step = sweep / steps
        var cur = startDeg
        for (i in 0..steps) {
            val rad = Math.toRadians(cur)
            val x = center.x + sin(rad) * r
            val y = center.y + cos(rad) * r
            pts += Vec2(x, y)
            cur += step
        }
        return pts
    }

    private fun escape(s: String): String = s.replace("\"", "\\\"")
}
