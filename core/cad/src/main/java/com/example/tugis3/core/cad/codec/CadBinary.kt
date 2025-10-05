package com.example.tugis3.core.cad.codec

import com.example.tugis3.core.cad.dto.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Deneysel binary paketleme + GZIP. Üretimde Protobuf / FlatBuffers tercih edilebilir.
 * Format:
 *  magic: 4 byte = 'C','A','D','B'
 *  version: u8 (DTO versiyonu referansı)
 *  count: u32
 *  repeated entity:
 *    type: u8 (1=PT,2=L,3=PL,4=PG,5=C,6=A,7=T)
 *    layerLen: u16, layer UTF-8 bytes
 *    payload -> type'e göre alanlar (double = 8 byte, boolean = u8)
 */
object CadBinary {
    private const val MAGIC = "CADB"
    private const val TYPE_PT: Byte = 1
    private const val TYPE_L: Byte = 2
    private const val TYPE_PL: Byte = 3
    private const val TYPE_PG: Byte = 4
    private const val TYPE_C: Byte = 5
    private const val TYPE_A: Byte = 6
    private const val TYPE_T: Byte = 7

    fun pack(list: List<CadEntityDto>, compress: Boolean = true): ByteArray {
        val raw = ByteArrayOutputStream()
        DataOutputStream(raw).use { out ->
            out.write(MAGIC.toByteArray(StandardCharsets.US_ASCII))
            out.writeByte(CAD_DTO_VERSION)
            out.writeInt(list.size)
            list.forEach { dto ->
                when (dto) {
                    is PointDto -> {
                        out.writeByte(TYPE_PT.toInt())
                        writeLayer(out, dto.layer)
                        out.writeDouble(dto.x); out.writeDouble(dto.y)
                    }
                    is LineDto -> {
                        out.writeByte(TYPE_L.toInt())
                        writeLayer(out, dto.layer)
                        out.writeDouble(dto.x1); out.writeDouble(dto.y1); out.writeDouble(dto.x2); out.writeDouble(dto.y2)
                    }
                    is PolylineDto -> {
                        out.writeByte(TYPE_PL.toInt())
                        writeLayer(out, dto.layer)
                        out.writeBoolean(dto.closed)
                        val n = dto.coords.size
                        out.writeInt(n)
                        dto.coords.forEach { out.writeDouble(it) }
                    }
                    is PolygonDto -> {
                        out.writeByte(TYPE_PG.toInt())
                        writeLayer(out, dto.layer)
                        out.writeInt(dto.rings.size)
                        dto.rings.forEach { ring ->
                            out.writeInt(ring.size)
                            ring.forEach { out.writeDouble(it) }
                        }
                    }
                    is CircleDto -> {
                        out.writeByte(TYPE_C.toInt())
                        writeLayer(out, dto.layer)
                        out.writeDouble(dto.cx); out.writeDouble(dto.cy); out.writeDouble(dto.r)
                    }
                    is ArcDto -> {
                        out.writeByte(TYPE_A.toInt())
                        writeLayer(out, dto.layer)
                        out.writeDouble(dto.cx); out.writeDouble(dto.cy); out.writeDouble(dto.r)
                        out.writeDouble(dto.startDeg); out.writeDouble(dto.endDeg)
                    }
                    is TextDto -> {
                        out.writeByte(TYPE_T.toInt())
                        writeLayer(out, dto.layer)
                        out.writeDouble(dto.x); out.writeDouble(dto.y); out.writeDouble(dto.h)
                        writeString(out, dto.text)
                        out.writeDouble(dto.rot)
                    }
                }
            }
        }
        val bytes = raw.toByteArray()
        return if (!compress) bytes else ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { it.write(bytes) }
            bos.toByteArray()
        }
    }

    fun unpack(data: ByteArray, compressed: Boolean = true): List<CadEntityDto> {
        val inputBytes = if (!compressed) data else GZIPInputStream(ByteArrayInputStream(data)).readAllBytes()
        val list = mutableListOf<CadEntityDto>()
        DataInputStream(ByteArrayInputStream(inputBytes)).use { inp ->
            val magicBytes = ByteArray(4); inp.readFully(magicBytes)
            val magic = String(magicBytes, StandardCharsets.US_ASCII)
            require(magic == MAGIC) { "Geçersiz magic: $magic" }
            val _ver = inp.readUnsignedByte() // şimdilik kullanılmıyor
            val count = inp.readInt()
            repeat(count) {
                val type = inp.readUnsignedByte().toByte()
                val layer = readLayer(inp)
                val dto: CadEntityDto = when (type) {
                    TYPE_PT -> PointDto(inp.readDouble(), inp.readDouble(), layer)
                    TYPE_L -> LineDto(inp.readDouble(), inp.readDouble(), inp.readDouble(), inp.readDouble(), layer)
                    TYPE_PL -> {
                        val closed = inp.readBoolean()
                        val n = inp.readInt()
                        val coords = DoubleArray(n) { inp.readDouble() }.toList()
                        PolylineDto(closed, coords, layer)
                    }
                    TYPE_PG -> {
                        val rc = inp.readInt()
                        val rings = (0 until rc).map {
                            val rn = inp.readInt()
                            DoubleArray(rn) { inp.readDouble() }.toList()
                        }
                        PolygonDto(rings, layer)
                    }
                    TYPE_C -> CircleDto(inp.readDouble(), inp.readDouble(), inp.readDouble(), layer)
                    TYPE_A -> ArcDto(inp.readDouble(), inp.readDouble(), inp.readDouble(), inp.readDouble(), inp.readDouble(), layer)
                    TYPE_T -> {
                        val x = inp.readDouble(); val y = inp.readDouble(); val h = inp.readDouble()
                        val txt = readString(inp)
                        val rot = inp.readDouble()
                        TextDto(x, y, h, txt, rot, layer)
                    }
                    else -> error("Bilinmeyen tip kodu $type")
                }
                list += dto
            }
        }
        return list
    }

    private fun writeLayer(out: DataOutputStream, layer: String) {
        val b = layer.toByteArray(StandardCharsets.UTF_8)
        require(b.size <= 0xFFFF) { "Layer adı çok uzun" }
        out.writeShort(b.size)
        out.write(b)
    }
    private fun readLayer(inp: DataInputStream): String {
        val len = inp.readUnsignedShort()
        val b = ByteArray(len)
        inp.readFully(b)
        return String(b, StandardCharsets.UTF_8)
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val b = value.toByteArray(StandardCharsets.UTF_8)
        out.writeInt(b.size)
        out.write(b)
    }
    private fun readString(inp: DataInputStream): String {
        val len = inp.readInt()
        val b = ByteArray(len)
        inp.readFully(b)
        return String(b, StandardCharsets.UTF_8)
    }
}

