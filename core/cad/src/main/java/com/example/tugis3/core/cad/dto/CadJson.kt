package com.example.tugis3.core.cad.dto

/**
 * Hafif, harici kütüphane kullanmadan JSON serileştirme (yalnızca DTO listesi için).
 * Format:
 * [ {"t":"LINE","layer":"L1", ...}, {...} ]
 * t (type) kodları: PT, L, PL, PG, C, A, T
 * NOT: Bu minimal serializer üretim için değil veri paylaşımı/demo amaçlıdır.
 */
object CadJson {
    // Yeni: üst seviye obje desteği {"v":1,"items":[...]} veya eski düz liste []
    fun serialize(list: List<CadEntityDto>, includeVersionWrapper: Boolean = false): String =
        if (includeVersionWrapper) buildString {
            append('{')
            append("\"v\":"); append(CAD_DTO_VERSION)
            append(",\"items\":")
            append(serializeBare(list))
            append('}')
        } else serializeBare(list)

    private fun serializeBare(list: List<CadEntityDto>): String = buildString {
        append('[')
        list.forEachIndexed { idx, dto ->
            if (idx > 0) append(',')
            append(dtoToJson(dto))
        }
        append(']')
    }

    private fun esc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun dtoToJson(dto: CadEntityDto): String = when(dto) {
        is PointDto -> "{" + "\"t\":\"PT\",\"layer\":\"${esc(dto.layer)}\",\"x\":${dto.x},\"y\":${dto.y}}"
        is LineDto -> "{" + "\"t\":\"L\",\"layer\":\"${esc(dto.layer)}\",\"x1\":${dto.x1},\"y1\":${dto.y1},\"x2\":${dto.x2},\"y2\":${dto.y2}}"
        is PolylineDto -> buildString {
            append('{'); append("\"t\":\"PL\",\"layer\":\"${esc(dto.layer)}\",\"closed\":"); append(dto.closed); append(",\"coords\":[")
            dto.coords.forEachIndexed { i,v -> if(i>0) append(','); append(v) }
            append("]}")
        }
        is PolygonDto -> buildString {
            append('{'); append("\"t\":\"PG\",\"layer\":\"${esc(dto.layer)}\",\"rings\":[")
            dto.rings.forEachIndexed { ri, ring ->
                if (ri>0) append(',')
                append('[')
                ring.forEachIndexed { i,v -> if(i>0) append(','); append(v) }
                append(']')
            }
            append("]}")
        }
        is CircleDto -> "{" + "\"t\":\"C\",\"layer\":\"${esc(dto.layer)}\",\"cx\":${dto.cx},\"cy\":${dto.cy},\"r\":${dto.r}}"
        is ArcDto -> "{" + "\"t\":\"A\",\"layer\":\"${esc(dto.layer)}\",\"cx\":${dto.cx},\"cy\":${dto.cy},\"r\":${dto.r},\"s\":${dto.startDeg},\"e\":${dto.endDeg}}"
        is TextDto -> buildString {
            append('{')
            append("\"t\":\"T\",\"layer\":\"${esc(dto.layer)}\",\"x\":${dto.x},\"y\":${dto.y},\"h\":${dto.h},\"text\":\"${esc(dto.text)}\"")
            if (dto.rot != 0.0) { append(",\"rot\":"); append(dto.rot) }
            append('}')
        }
    }

    fun parse(json: String): List<CadEntityDto> {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith('{')) parseWrapper(trimmed) else parseArray(trimmed)
    }

    private fun parseArray(arr: String): List<CadEntityDto> {
        val trimmed = arr.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val items = mutableListOf<String>()
        var depth = 0
        var inStr = false
        val sb = StringBuilder()
        for (i in 1 until trimmed.length-1) {
            val c = trimmed[i]
            if (c=='"' && trimmed.getOrNull(i-1)!='\\') inStr = !inStr
            if (!inStr) {
                if (c=='{') depth++ else if (c=='}') depth--
                if (c==',' && depth==0) {
                    val seg = sb.toString().trim()
                    if (seg.isNotEmpty()) items += seg
                    sb.setLength(0)
                    continue
                }
            }
            sb.append(c)
        }
        val last = sb.toString().trim()
        if (last.isNotEmpty()) items += last
        return items.mapNotNull { parseObject(it) }
    }

    private fun parseWrapper(obj: String): List<CadEntityDto> {
        // Basit anahtar yakalama: v ve items
        val vMatch = Regex("\"v\"\\s*:\\s*(\\d+)").find(obj)
        val _version = vMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0 // şimdilik kullanılmıyor
        val itemsMatch = Regex("\"items\"\\s*:\\s*(\\[.*])", RegexOption.DOT_MATCHES_ALL).find(obj)
        val arr = itemsMatch?.groupValues?.get(1) ?: return emptyList()
        return parseArray(arr)
    }

    private fun parseObject(obj: String): CadEntityDto {
        fun kv(key: String): String? {
            val r = Regex("\"$key\"\\s*:\\s*(\"([^\\\"]|\\.)*\"|[-0-9.truefalsenull]+|\\[[^]]*]|\\{[^}]*})")
            val m = r.find(obj) ?: return null
            val raw = m.groupValues[1].trim()
            return if (raw.startsWith('"')) raw.drop(1).dropLast(1).replace("\\\"","\"").replace("\\\\","\\") else raw
        }
        val type = kv("t") ?: return error("type yok")
        val layer = kv("layer") ?: "default"
        return when(type) {
            "PT" -> PointDto(kv("x")?.toDoubleOrNull() ?: 0.0, kv("y")?.toDoubleOrNull() ?: 0.0, layer)
            "L" -> LineDto(
                kv("x1")?.toDoubleOrNull() ?: 0.0,
                kv("y1")?.toDoubleOrNull() ?: 0.0,
                kv("x2")?.toDoubleOrNull() ?: 0.0,
                kv("y2")?.toDoubleOrNull() ?: 0.0,
                layer
            )
            "PL" -> {
                val closed = kv("closed")?.toBoolean() ?: false
                val coordsRaw = kv("coords") ?: "[]"
                val nums = extractNumberArray(coordsRaw)
                PolylineDto(closed, nums, layer)
            }
            "PG" -> {
                val ringsRaw = kv("rings") ?: "[]"
                val rings = extractNestedNumberArrays(ringsRaw)
                PolygonDto(rings.ifEmpty { listOf(emptyList()) }, layer) // boş gelirse sentinel
            }
            "C" -> CircleDto(
                kv("cx")?.toDoubleOrNull() ?: 0.0,
                kv("cy")?.toDoubleOrNull() ?: 0.0,
                kv("r")?.toDoubleOrNull() ?: 0.0,
                layer
            )
            "A" -> ArcDto(
                kv("cx")?.toDoubleOrNull() ?: 0.0,
                kv("cy")?.toDoubleOrNull() ?: 0.0,
                kv("r")?.toDoubleOrNull() ?: 0.0,
                kv("s")?.toDoubleOrNull() ?: 0.0,
                kv("e")?.toDoubleOrNull() ?: 0.0,
                layer
            )
            "T" -> TextDto(
                kv("x")?.toDoubleOrNull() ?: 0.0,
                kv("y")?.toDoubleOrNull() ?: 0.0,
                kv("h")?.toDoubleOrNull() ?: 0.0,
                kv("text") ?: "",
                kv("rot")?.toDoubleOrNull() ?: 0.0,
                layer
            )
            else -> error("Bilinmeyen t: $type")
        }
    }

    private fun extractNumberArray(raw: String): List<Double> {
        val inner = raw.trim().removePrefix("[").removeSuffix("]").trim()
        if (inner.isEmpty()) return emptyList()
        return inner.split(',').mapNotNull { it.trim().toDoubleOrNull() }
    }

    private fun extractNestedNumberArrays(raw: String): List<List<Double>> {
        val t = raw.trim()
        if (t == "[]") return emptyList()
        if (!(t.startsWith('[') && t.endsWith(']'))) return emptyList()
        val out = mutableListOf<List<Double>>()
        var depth = 0
        var inStr = false
        val sb = StringBuilder()
        for (i in 1 until t.length-1) {
            val c = t[i]
            if (c=='"' && t.getOrNull(i-1)!='\\') inStr = !inStr
            if (!inStr) {
                if (c=='[') depth++ else if (c==']') depth--
                if (c==',' && depth==0) {
                    val seg = sb.toString().trim()
                    if (seg.isNotEmpty()) out += extractNumberArray(seg)
                    sb.setLength(0)
                    continue
                }
            }
            sb.append(c)
        }
        val last = sb.toString().trim()
        if (last.isNotEmpty()) out += extractNumberArray(last)
        return out
    }
}
