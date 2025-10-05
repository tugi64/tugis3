package com.example.tugis3.core.cad.parser

import com.example.tugis3.core.cad.model.*

@Deprecated("Kapsamlı DxfParser kullanın; bu basit ASCII parser sadece LINE ve LWPOLYLINE döndürür.")
class DxfAsciiParser {
    fun parse(text: String): List<CadEntity> {
        val lines = text.lines()
        val pairs = mutableListOf<Pair<String,String>>()
        var i = 0
        while (i + 1 < lines.size) {
            pairs += lines[i].trim() to lines[i+1].trim()
            i += 2
        }
        return parsePairs(pairs)
    }

    fun parse(stream: java.io.InputStream): List<CadEntity> =
        parse(stream.reader().readText())

    private fun parsePairs(pairs: List<Pair<String, String>>): List<CadEntity> {
        val out = mutableListOf<CadEntity>()
        var curType: String? = null
        var layer: String = "0"
        var x1: Double? = null; var y1: Double? = null; var x2: Double? = null; var y2: Double? = null
        var poly: MutableList<Point>? = null
        var closed = false

        fun flush() {
            when (curType) {
                "LINE" -> if (x1!=null && y1!=null && x2!=null && y2!=null) out += CadLine(Point(x1!!, y1!!), Point(x2!!, y2!!), layer = layer)
                "LWPOLYLINE" -> if (!poly.isNullOrEmpty()) out += CadPolyline(points = poly!!.toList(), isClosed = closed, layer = layer)
            }
            x1=null; y1=null; x2=null; y2=null; poly=null; closed=false
        }

        var i=0
        while (i < pairs.size) {
            val (code,value) = pairs[i]
            when(code) {
                "0" -> {
                    if (value.equals("EOF", true)) { flush(); break }
                    if (value.equals("ENDSEC", true)) { flush(); curType=null }
                    else { flush(); curType = value.uppercase() }
                }
                "8" -> layer = value
                "10" -> when(curType){
                    "LINE" -> x1 = value.toDoubleOrNull().takeIf{ x1==null } ?: x1
                    "LWPOLYLINE" -> {
                        val x = value.toDoubleOrNull(); val nxt = pairs.getOrNull(i+1)
                        if (x!=null && nxt?.first=="20") {
                            val y = nxt.second.toDoubleOrNull()
                            if (y!=null) { if (poly==null) poly = mutableListOf(); poly!!.add(Point(x,y)) }
                        }
                    }
                }
                "20" -> if (curType=="LINE" && y1==null) y1 = value.toDoubleOrNull()
                "11" -> if (curType=="LINE") x2 = value.toDoubleOrNull()
                "21" -> if (curType=="LINE") y2 = value.toDoubleOrNull()
                "70" -> if (curType=="LWPOLYLINE") { val f = value.toIntOrNull()?:0; closed = (f and 1)!=0 }
            }
            i++
        }
        flush()
        return out
    }
}
