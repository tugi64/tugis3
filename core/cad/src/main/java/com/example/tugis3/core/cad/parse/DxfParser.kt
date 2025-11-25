package com.example.tugis3.core.cad.parse

import com.example.tugis3.core.cad.model.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Çok basit DXF parser: LINE, LWPOLYLINE, CIRCLE, ARC, TEXT destekler. 2D ölçüm amaçlı minimal.
 * Üretim kullanımı için kapsamlı bir kütüphane tercih edilmelidir.
 */
class DxfParser {
    fun parse(stream: InputStream): List<CadEntity> = stream.use { s ->
        val br = BufferedReader(InputStreamReader(s))
        val lines = br.readLines()
        val out = mutableListOf<CadEntity>()
        var i = 0
        var unsupportedCount = 0
        val unsupportedTypes = mutableSetOf<String>()

        fun nextPair(): Pair<String, String>? {
            if (i + 1 >= lines.size) return null
            val code = lines[i].trim(); val value = lines[i + 1].trim(); i += 2
            return code to value
        }

        android.util.Log.d("DXF", "Parse başladı: ${lines.size} satır")

        while (true) {
            val p = nextPair() ?: break
            if (p.first == "0") {
                val entityType = p.second.uppercase()
                when (entityType) {
                    "ENDSEC", "EOF" -> {
                        android.util.Log.d("DXF", "Section sonu: $entityType")
                        break
                    }
                    "LINE" -> parseLine(::nextPair)?.let {
                        out.add(it)
                        if (out.size % 100 == 0) android.util.Log.d("DXF", "İşlenen entity: ${out.size}")
                    }
                    "LWPOLYLINE", "POLYLINE" -> parseLwPolyline(::nextPair)?.let {
                        out.add(it)
                        if (out.size % 100 == 0) android.util.Log.d("DXF", "İşlenen entity: ${out.size}")
                    }
                    "CIRCLE" -> parseCircle(::nextPair)?.let {
                        out.add(it)
                        if (out.size % 100 == 0) android.util.Log.d("DXF", "İşlenen entity: ${out.size}")
                    }
                    "ARC" -> parseArc(::nextPair)?.let {
                        out.add(it)
                        if (out.size % 100 == 0) android.util.Log.d("DXF", "İşlenen entity: ${out.size}")
                    }
                    "TEXT", "MTEXT" -> parseText(::nextPair)?.let {
                        out.add(it)
                        if (out.size % 100 == 0) android.util.Log.d("DXF", "İşlenen entity: ${out.size}")
                    }
                    "SECTION", "HEADER", "CLASSES", "TABLES", "BLOCKS", "ENTITIES", "OBJECTS" -> {
                        // Section başlıkları - devam et
                    }
                    else -> {
                        // Desteklenmeyen entity
                        unsupportedCount++
                        unsupportedTypes.add(entityType)
                    }
                }
            }
        }

        android.util.Log.i("DXF", "Parse tamamlandı: ${out.size} entity yüklendi")
        if (unsupportedCount > 0) {
            android.util.Log.w("DXF", "Desteklenmeyen ${unsupportedCount} entity atlandı: ${unsupportedTypes.joinToString()}")
        }

        out
    }

    private fun parseLine(next: () -> Pair<String, String>?): CadLine? {
        var x1: Double? = null; var y1: Double? = null; var x2: Double? = null; var y2: Double? = null
        var layer = "0"; var color: Int? = null
        while (true) {
            val p = next() ?: break
            if (p.first == "0") {
                return if (x1 != null && y1 != null && x2 != null && y2 != null)
                    CadLine(Point(x1!!, y1!!), Point(x2!!, y2!!), layer = layer, colorIndex = color) else null
            }
            when (p.first) {
                "8" -> layer = p.second
                "62" -> color = p.second.toIntOrNull()
                "10" -> x1 = p.second.toDoubleOrNull()
                "20" -> y1 = p.second.toDoubleOrNull()
                "11" -> x2 = p.second.toDoubleOrNull()
                "21" -> y2 = p.second.toDoubleOrNull()
            }
            if (x1 != null && y1 != null && x2 != null && y2 != null) {
                return CadLine(Point(x1!!, y1!!), Point(x2!!, y2!!), layer = layer, colorIndex = color)
            }
        }
        return null
    }

    private fun parseLwPolyline(next: () -> Pair<String, String>?): CadPolyline? {
        val pts = mutableListOf<Point>()
        var closed = false
        val temp = mutableMapOf<Int, Double>()
        var layer = "0"; var color: Int? = null
        while (true) {
            val p = next() ?: break
            if (p.first == "0") {
                if (temp.containsKey(10) && temp.containsKey(20)) {
                    pts.add(Point(temp[10]!!, temp[20]!!))
                }
                return CadPolyline(points = pts.toList(), isClosed = closed, layer = layer, colorIndex = color)
            }
            when (p.first) {
                "8" -> layer = p.second
                "62" -> color = p.second.toIntOrNull()
                "10" -> temp[10] = p.second.toDoubleOrNull() ?: return null
                "20" -> {
                    temp[20] = p.second.toDoubleOrNull() ?: return null
                    if (temp.containsKey(10)) {
                        pts.add(Point(temp[10]!!, temp[20]!!))
                        temp.remove(10); temp.remove(20)
                    }
                }
                "70" -> { val flag = p.second.toIntOrNull() ?: 0; closed = (flag and 1) != 0 }
            }
        }
        if (pts.isNotEmpty()) return CadPolyline(points = pts, isClosed = closed, layer = layer, colorIndex = color)
        return null
    }

    private fun parseCircle(next: () -> Pair<String, String>?): CadCircle? {
        var cx: Double? = null; var cy: Double? = null; var r: Double? = null
        var layer = "0"; var color: Int? = null
        while (true) {
            val p = next() ?: break
            if (p.first == "0") {
                return if (cx != null && cy != null && r != null) CadCircle(Point(cx!!, cy!!), r!!, layer = layer, colorIndex = color) else null
            }
            when (p.first) {
                "8" -> layer = p.second
                "62" -> color = p.second.toIntOrNull()
                "10" -> cx = p.second.toDoubleOrNull()
                "20" -> cy = p.second.toDoubleOrNull()
                "40" -> r = p.second.toDoubleOrNull()
            }
            if (cx != null && cy != null && r != null) {
                return CadCircle(Point(cx!!, cy!!), r!!, layer = layer, colorIndex = color)
            }
        }
        return null
    }

    private fun parseArc(next: () -> Pair<String, String>?): CadArc? {
        var cx: Double? = null; var cy: Double? = null; var r: Double? = null
        var a1: Double? = null; var a2: Double? = null
        var layer = "0"; var color: Int? = null
        while (true) {
            val p = next() ?: break
            if (p.first == "0") {
                return if (cx != null && cy != null && r != null && a1 != null && a2 != null)
                    CadArc(Point(cx!!, cy!!), r!!, a1!!, a2!!, layer = layer, colorIndex = color) else null
            }
            when (p.first) {
                "8" -> layer = p.second
                "62" -> color = p.second.toIntOrNull()
                "10" -> cx = p.second.toDoubleOrNull()
                "20" -> cy = p.second.toDoubleOrNull()
                "40" -> r = p.second.toDoubleOrNull()
                "50" -> a1 = p.second.toDoubleOrNull()
                "51" -> a2 = p.second.toDoubleOrNull()
            }
            if (cx != null && cy != null && r != null && a1 != null && a2 != null) {
                return CadArc(Point(cx!!, cy!!), r!!, a1!!, a2!!, layer = layer, colorIndex = color)
            }
        }
        return null
    }

    private fun parseText(next: () -> Pair<String, String>?): CadText? {
        var x: Double? = null; var y: Double? = null; var h: Double? = null; var txt: String? = null
        var layer = "0"; var color: Int? = null
        while (true) {
            val p = next() ?: break
            if (p.first == "0") {
                return if (x != null && y != null && h != null && txt != null)
                    CadText(Point(x!!, y!!), h!!, txt!!, layer = layer, colorIndex = color) else null
            }
            when (p.first) {
                "8" -> layer = p.second
                "62" -> color = p.second.toIntOrNull()
                "10" -> x = p.second.toDoubleOrNull()
                "20" -> y = p.second.toDoubleOrNull()
                "40" -> h = p.second.toDoubleOrNull()
                "1" -> txt = p.second
            }
            if (x != null && y != null && h != null && txt != null) {
                return CadText(Point(x!!, y!!), h!!, txt!!, layer = layer, colorIndex = color)
            }
        }
        return null
    }
}
