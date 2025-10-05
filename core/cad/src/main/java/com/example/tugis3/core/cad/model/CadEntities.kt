@file:Suppress("unused")
package com.example.tugis3.core.cad.model

// CAD katmanı temel sealed hiyerarşi
// Amaç: Çizim / depolama / render pipeline'ında tek tip domain modeli sağlamak.
// NOT: Adım 1 - Sadece hiyerarşi. Hesaplamalar (alan/uzunluk vs.) ayrı utils (Adım 2).

/**
 * CAD varlık türleri (persist / serialize discriminator).
 */
enum class CadEntityType { POINT, LINE, POLYLINE, POLYGON, TEXT, CIRCLE, ARC }

// Geri uyumluluk için eski Point adı:
typealias Point = Vec2

/** Basit 2B vektör. */
data class Vec2(val x: Double, val y: Double) {
    fun toFloatPair(): Pair<Float, Float> = x.toFloat() to y.toFloat()
}

/**
 * Eksen hizalı bounding box.
 */
data class BoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    init {
        require(minX <= maxX && minY <= maxY) { "Geçersiz BoundingBox: ($minX,$minY)-($maxX,$maxY)" }
    }
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val center: Vec2 get() = Vec2(minX + width / 2.0, minY + height / 2.0)
    fun expand(pad: Double): BoundingBox = BoundingBox(minX - pad, minY - pad, maxX + pad, maxY + pad)
    fun union(other: BoundingBox): BoundingBox = BoundingBox(
        minX = minOf(minX, other.minX),
        minY = minOf(minY, other.minY),
        maxX = maxOf(maxX, other.maxX),
        maxY = maxOf(maxY, other.maxY)
    )
    companion object {
        val EMPTY = BoundingBox(0.0, 0.0, 0.0, 0.0)
        fun ofPoints(points: Iterable<Vec2>): BoundingBox {
            val it = points.iterator()
            if (!it.hasNext()) return EMPTY
            var minX: Double; var maxX: Double; var minY: Double; var maxY: Double
            it.next().also { p ->
                minX = p.x; maxX = p.x; minY = p.y; maxY = p.y
            }
            while (it.hasNext()) {
                val p = it.next()
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            return BoundingBox(minX, minY, maxX, maxY)
        }
    }
}

/** Stil (renkler ARGB int). */
data class CadStyle(
    val strokeColor: Int? = null,
    val strokeWidth: Float? = null,
    val fillColor: Int? = null,
    val textSizeSp: Float? = null,
    val fontFamily: String? = null
)

/** Tüm CAD varlıklarının ortak kontratı. */
sealed interface CadEntity {
    val layer: String
    val attrs: Map<String, String>
    val style: CadStyle?
    val colorIndex: Int? // Layer palet indeks veya null
    val type: CadEntityType
    fun bounds(): BoundingBox
}

/** Nokta. */
data class CadPoint(
    val position: Vec2,
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    override val type: CadEntityType = CadEntityType.POINT
    override fun bounds(): BoundingBox = BoundingBox(position.x, position.y, position.x, position.y)
}

/** İki nokta arasındaki doğru. */
data class CadLine(
    val start: Vec2,
    val end: Vec2,
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    override val type: CadEntityType = CadEntityType.LINE
    override fun bounds(): BoundingBox = BoundingBox(
        minOf(start.x, end.x),
        minOf(start.y, end.y),
        maxOf(start.x, end.x),
        maxOf(start.y, end.y)
    )
}

/** Çoklu çizgi (polyline). En az 2 nokta. */
data class CadPolyline(
    val points: List<Vec2>,
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null,
    val isClosed: Boolean = false
) : CadEntity {
    init { require(points.size >= 2) { "Polyline en az 2 nokta içermeli" } }
    override val type: CadEntityType = CadEntityType.POLYLINE
    override fun bounds(): BoundingBox = BoundingBox.ofPoints(points)
}

/** Poligon (alan). İlk ve son nokta otomatik kapatılmış varsayılır. Holes listesinde iç boşluklar tutulur. */
data class CadPolygon(
    val rings: List<List<Vec2>>, // ring[0] = outer, ring[1..n] = holes (opsiyonel)
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    init {
        require(rings.isNotEmpty()) { "Poligon en az bir ring içermeli" }
        require(rings.all { it.size >= 3 }) { "Her ring en az 3 nokta içermeli" }
    }
    override val type: CadEntityType = CadEntityType.POLYGON
    override fun bounds(): BoundingBox = BoundingBox.ofPoints(rings.first())
}

/** Metin etiketi. */
data class CadText(
    val position: Vec2,
    val height: Double,
    val text: String,
    val rotationDeg: Double = 0.0, // yeni: metin rotasyonu (0 = yatay)
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    override val type: CadEntityType = CadEntityType.TEXT
    override fun bounds(): BoundingBox = BoundingBox(position.x, position.y, position.x, position.y)
}

/** Daire (merkez + yarıçap). */
data class CadCircle(
    val center: Vec2,
    val radius: Double,
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    init { require(radius > 0) { "Yarıçap > 0 olmalı" } }
    override val type: CadEntityType = CadEntityType.CIRCLE
    override fun bounds(): BoundingBox = BoundingBox(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
    )
}

/** Daire yay (arc). */
data class CadArc(
    val center: Vec2,
    val radius: Double,
    val startAngleDeg: Double,
    val endAngleDeg: Double,
    override val layer: String = "default",
    override val attrs: Map<String, String> = emptyMap(),
    override val style: CadStyle? = null,
    override val colorIndex: Int? = null
) : CadEntity {
    init { require(radius > 0) { "Yarıçap > 0 olmalı" } }
    override val type: CadEntityType = CadEntityType.ARC
    override fun bounds(): BoundingBox = arcBoundingBox(center, radius, startAngleDeg, endAngleDeg)
}

private fun normalizeDeg(a: Double): Double {
    var v = a % 360.0
    if (v < 0) v += 360.0
    return v
}

private fun sweepContains(angle: Double, start: Double, end: Double): Boolean {
    val a = normalizeDeg(angle)
    val s = normalizeDeg(start)
    var e = normalizeDeg(end)
    if (s == e) return true // tam çember varsay
    if (e < s) e += 360.0
    val aa = if (a < s) a + 360.0 else a
    return aa in s..e
}

private fun arcBoundingBox(center: Vec2, r: Double, start: Double, end: Double): BoundingBox {
    // Kandidat açılar: start, end + kardinal (0,90,180,270)
    val candidates = mutableListOf<Double>()
    candidates += start
    candidates += end
    listOf(0.0, 90.0, 180.0, 270.0).forEach { c -> if (sweepContains(c, start, end)) candidates += c }
    // Noktaları hesapla
    val pts = candidates.distinct().map { ang ->
        val rad = Math.toRadians(normalizeDeg(ang))
        Vec2(center.x + r * kotlin.math.cos(rad), center.y + r * kotlin.math.sin(rad))
    }
    return if (pts.isEmpty()) BoundingBox(center.x, center.y, center.x, center.y) else BoundingBox.ofPoints(pts)
}

// İlerleyen adımlarda (2+) için: geometry utils, mapper, DB vs. ayrı dosyalarda tanımlanacak.
