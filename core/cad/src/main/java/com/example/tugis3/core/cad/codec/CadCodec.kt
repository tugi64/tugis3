package com.example.tugis3.core.cad.codec

import com.example.tugis3.core.cad.model.*

/**
 * CAD entity encode/decode (persist / export pipeline).
 * Type codes (backwards compatible with önceki şema):
 *  L  -> Line
 *  PL -> Polyline
 *  C  -> Circle
 *  A  -> Arc
 *  T  -> Text
 *  PG -> Polygon (yeni)
 * (Opsiyonel gelecekte: PT -> Tek nokta; şimdilik DB için gerek yoktu.)
 *
 * dataEncoded formatları (pipe '|' ayracı):
 *  L : x1|y1|x2|y2
 *  PL: closedFlag(0/1)|x1|y1|x2|y2|... (ardışık noktalar)
 *  C : cx|cy|r
 *  A : cx|cy|r|startDeg|endDeg
 *  T : x|y|height|escapedText ( '|' => %7C )
 *  PG: ringCount|ring1Size|x1|y1|...|ring2Size|...  (her ring için önce nokta sayısı sonra koordinatlar)
 */
object CadCodec {
    fun encode(entity: CadEntity): Pair<String, String> = when(entity) {
        is CadLine -> "L" to listOf(entity.start.x, entity.start.y, entity.end.x, entity.end.y).joinToString("|")
        is CadPolyline -> "PL" to buildString {
            append(if (entity.isClosed) 1 else 0)
            entity.points.forEach { append('|'); append(it.x); append('|'); append(it.y) }
        }
        is CadCircle -> "C" to listOf(entity.center.x, entity.center.y, entity.radius).joinToString("|")
        is CadArc -> "A" to listOf(entity.center.x, entity.center.y, entity.radius, entity.startAngleDeg, entity.endAngleDeg).joinToString("|")
        is CadText -> {
            // Geri uyumluluk: rotasyon 0.0 ise eski format (4 parça), değilse 5 parça
            if (entity.rotationDeg == 0.0) {
                "T" to listOf(entity.position.x, entity.position.y, entity.height, escape(entity.text)).joinToString("|")
            } else {
                "T" to listOf(entity.position.x, entity.position.y, entity.height, escape(entity.text), entity.rotationDeg).joinToString("|")
            }
        }
        is CadPolygon -> encodePolygon(entity)
        is CadPoint -> "PT" to listOf(entity.position.x, entity.position.y).joinToString("|") // şimdilik decode opsiyonel
    }

    private fun encodePolygon(pg: CadPolygon): Pair<String, String> {
        val rings = pg.rings
        val payload = buildString {
            append(rings.size)
            rings.forEach { ring ->
                append('|'); append(ring.size)
                ring.forEach { p -> append('|'); append(p.x); append('|'); append(p.y) }
            }
        }
        return "PG" to payload
    }

    fun decode(type: String, data: String, layer: String, colorIndex: Int?): CadEntity? = when(type) {
        "L" -> decodeLine(data, layer, colorIndex)
        "PL" -> decodePolyline(data, layer, colorIndex)
        "C" -> decodeCircle(data, layer, colorIndex)
        "A" -> decodeArc(data, layer, colorIndex)
        "T" -> decodeText(data, layer, colorIndex)
        "PG" -> decodePolygon(data, layer, colorIndex)
        // "PT" -> decodePoint(data, layer, colorIndex) // gerekirse aç
        else -> null
    }

    private fun decodeLine(data: String, layer: String, colorIndex: Int?): CadLine? {
        val parts = data.split('|')
        return if (parts.size >= 4) CadLine(Vec2(parts[0].toDouble(), parts[1].toDouble()), Vec2(parts[2].toDouble(), parts[3].toDouble()), layer = layer, colorIndex = colorIndex) else null
    }

    private fun decodePolyline(data: String, layer: String, colorIndex: Int?): CadPolyline? {
        val parts = data.split('|')
        if (parts.isEmpty()) return null
        val closed = parts[0] == "1"
        val coords = parts.drop(1)
        if (coords.size % 2 != 0) return null
        val pts = coords.chunked(2).map { Vec2(it[0].toDouble(), it[1].toDouble()) }
        return CadPolyline(points = pts, isClosed = closed, layer = layer, colorIndex = colorIndex)
    }

    private fun decodeCircle(data: String, layer: String, colorIndex: Int?): CadCircle? {
        val p = data.split('|')
        return if (p.size >= 3) CadCircle(Vec2(p[0].toDouble(), p[1].toDouble()), p[2].toDouble(), layer = layer, colorIndex = colorIndex) else null
    }

    private fun decodeArc(data: String, layer: String, colorIndex: Int?): CadArc? {
        val p = data.split('|')
        return if (p.size >= 5) CadArc(Vec2(p[0].toDouble(), p[1].toDouble()), p[2].toDouble(), p[3].toDouble(), p[4].toDouble(), layer = layer, colorIndex = colorIndex) else null
    }

    private fun decodeText(data: String, layer: String, colorIndex: Int?): CadText? {
        val p = data.split('|')
        // 4 alan: x|y|h|text  / 5 alan: x|y|h|text|rot
        return if (p.size >= 4) {
            val rot = if (p.size >= 5) p[4].toDoubleOrNull() ?: 0.0 else 0.0
            CadText(Vec2(p[0].toDouble(), p[1].toDouble()), p[2].toDouble(), unescape(p[3]), rotationDeg = rot, layer = layer, colorIndex = colorIndex)
        } else null
    }

    private fun decodePolygon(data: String, layer: String, colorIndex: Int?): CadPolygon? {
        val parts = data.split('|')
        if (parts.isEmpty()) return null
        val ringCount = parts[0].toIntOrNull() ?: return null
        var idx = 1
        val rings = mutableListOf<List<Vec2>>()
        repeat(ringCount) {
            if (idx >= parts.size) return null
            val len = parts[idx].toIntOrNull() ?: return null
            idx++
            val needed = len * 2
            if (idx + needed > parts.size) return null
            val ring = mutableListOf<Vec2>()
            var j = 0
            while (j < needed) {
                val x = parts[idx + j].toDoubleOrNull() ?: return null
                val y = parts[idx + j + 1].toDoubleOrNull() ?: return null
                ring += Vec2(x, y)
                j += 2
            }
            rings += ring
            idx += needed
        }
        if (rings.isEmpty()) return null
        return CadPolygon(rings = rings, layer = layer, colorIndex = colorIndex)
    }

    private fun escape(t: String) = t.replace("|", "%7C")
    private fun unescape(t: String) = t.replace("%7C", "|")
}
