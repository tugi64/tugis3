package com.example.tugis3.model

import androidx.compose.ui.graphics.Color

/**
 * CAD Katman (Layer) - Kullanım kılavuzu bölüm 6.3.1
 * Her katman farklı renk, kalınlık ve obje tipine sahip olabilir
 */
data class CADLayer(
    val id: String,
    val name: String,
    val color: Color,
    val lineWidth: Float = 2f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val description: String = ""
)

/**
 * CAD Obje Tipi - Kullanım kılavuzu bölüm 6.3.3
 */
enum class CADObjectType {
    POINT,      // Nokta
    LINE,       // Çizgi
    POLYLINE,   // Çoklu çizgi
    POLYGON,    // Çokgen
    CIRCLE,     // Daire
    ARC,        // Yay
    TEXT        // Metin
}

/**
 * CAD Obje - Temel CAD elemanı
 */
sealed class CADObject {
    abstract val id: String
    abstract val layerId: String
    abstract val type: CADObjectType
    abstract val properties: Map<String, String>

    data class Point(
        override val id: String,
        override val layerId: String,
        val x: Double,
        val y: Double,
        val z: Double = 0.0,
        val name: String = "",
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.POINT
    }

    data class Line(
        override val id: String,
        override val layerId: String,
        val startX: Double,
        val startY: Double,
        val endX: Double,
        val endY: Double,
        val startZ: Double = 0.0,
        val endZ: Double = 0.0,
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.LINE
    }

    data class Polyline(
        override val id: String,
        override val layerId: String,
        val points: List<CADPoint>,
        val isClosed: Boolean = false,
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.POLYLINE

        fun getLength(): Double {
            var length = 0.0
            for (i in 0 until points.size - 1) {
                length += points[i].distanceTo(points[i + 1])
            }
            if (isClosed && points.size > 2) {
                length += points.last().distanceTo(points.first())
            }
            return length
        }

        fun getArea(): Double {
            if (points.size < 3 || !isClosed) return 0.0

            var area = 0.0
            for (i in points.indices) {
                val j = (i + 1) % points.size
                area += points[i].x * points[j].y
                area -= points[j].x * points[i].y
            }
            return kotlin.math.abs(area / 2.0)
        }
    }

    data class Polygon(
        override val id: String,
        override val layerId: String,
        val points: List<CADPoint>,
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.POLYGON

        fun getPerimeter(): Double {
            var perimeter = 0.0
            for (i in points.indices) {
                val j = (i + 1) % points.size
                perimeter += points[i].distanceTo(points[j])
            }
            return perimeter
        }

        fun getArea(): Double {
            if (points.size < 3) return 0.0

            var area = 0.0
            for (i in points.indices) {
                val j = (i + 1) % points.size
                area += points[i].x * points[j].y
                area -= points[j].x * points[i].y
            }
            return kotlin.math.abs(area / 2.0)
        }
    }

    data class Circle(
        override val id: String,
        override val layerId: String,
        val centerX: Double,
        val centerY: Double,
        val radius: Double,
        val centerZ: Double = 0.0,
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.CIRCLE

        fun getCircumference(): Double = 2 * kotlin.math.PI * radius
        fun getArea(): Double = kotlin.math.PI * radius * radius
    }

    data class Text(
        override val id: String,
        override val layerId: String,
        val text: String,
        val x: Double,
        val y: Double,
        val z: Double = 0.0,
        val fontSize: Float = 12f,
        val rotation: Float = 0f,
        override val properties: Map<String, String> = emptyMap()
    ) : CADObject() {
        override val type = CADObjectType.TEXT
    }
}

/**
 * CAD Nokta - 3D koordinat
 */
data class CADPoint(
    val x: Double,
    val y: Double,
    val z: Double = 0.0
) {
    fun distanceTo(other: CADPoint): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceTo2D(other: CADPoint): Double {
        val dx = other.x - x
        val dy = other.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * CAD Proje - Tüm katmanları ve objeleri içerir
 */
data class CADProject(
    val id: String,
    val name: String,
    val layers: List<CADLayer> = emptyList(),
    val objects: List<CADObject> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    fun getObjectsByLayer(layerId: String): List<CADObject> {
        return objects.filter { it.layerId == layerId }
    }

    fun getVisibleObjects(): List<CADObject> {
        val visibleLayerIds = layers.filter { it.isVisible }.map { it.id }.toSet()
        return objects.filter { it.layerId in visibleLayerIds }
    }
}

/**
 * Aplikasyon Tipi - Kullanım kılavuzu bölüm 6.4-6.6
 */
enum class StakeoutType {
    POINT,      // Nokta aplikasyonu (Point Stakeout)
    LINE,       // Hat aplikasyonu (Line Stakeout)
    CAD,        // CAD aplikasyonu (CAD Stakeout)
    OFFSET      // Ofset aplikasyonu (Offset Stakeout)
}

/**
 * Hat Aplikasyon Verisi - Kullanım kılavuzu bölüm 6.5
 */
data class LineStakeout(
    val id: String,
    val name: String,
    val startPoint: CADPoint,
    val endPoint: CADPoint,
    val offset: Double = 0.0,  // Hattan sağa/sola kayma (m)
    val stations: List<Double> = emptyList(),  // İstasyon noktaları
    val properties: Map<String, String> = emptyMap()
) {
    fun getLength(): Double = startPoint.distanceTo(endPoint)

    fun getPointAtDistance(distance: Double): CADPoint {
        val totalLength = getLength()
        if (distance <= 0) return startPoint
        if (distance >= totalLength) return endPoint

        val ratio = distance / totalLength
        return CADPoint(
            x = startPoint.x + (endPoint.x - startPoint.x) * ratio,
            y = startPoint.y + (endPoint.y - startPoint.y) * ratio,
            z = startPoint.z + (endPoint.z - startPoint.z) * ratio
        )
    }

    fun getPointWithOffset(distance: Double, offsetDistance: Double): CADPoint {
        val pointOnLine = getPointAtDistance(distance)

        // Hat yönü vektörü
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy)

        // Normalize
        val ndx = dx / length
        val ndy = dy / length

        // Dik vektör (sağa 90 derece döndür)
        val perpX = -ndy
        val perpY = ndx

        return CADPoint(
            x = pointOnLine.x + perpX * offsetDistance,
            y = pointOnLine.y + perpY * offsetDistance,
            z = pointOnLine.z
        )
    }

    fun getClosestPointOnLine(currentX: Double, currentY: Double): Pair<CADPoint, Double> {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y

        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) {
            return Pair(startPoint, 0.0)
        }

        val t = ((currentX - startPoint.x) * dx + (currentY - startPoint.y) * dy) / lengthSquared
        val clampedT = t.coerceIn(0.0, 1.0)

        val closestPoint = CADPoint(
            x = startPoint.x + clampedT * dx,
            y = startPoint.y + clampedT * dy,
            z = startPoint.z + clampedT * (endPoint.z - startPoint.z)
        )

        val distance = clampedT * getLength()
        return Pair(closestPoint, distance)
    }
}

/**
 * Kesit (Cross Section) Verisi - Yol kesiti
 */
data class CrossSection(
    val station: Double,
    val centerLinePoint: CADPoint,
    val leftPoints: List<CADPoint> = emptyList(),
    val rightPoints: List<CADPoint> = emptyList(),
    val cutArea: Double = 0.0,      // Kazı alanı
    val fillArea: Double = 0.0,     // Dolgu alanı
    val properties: Map<String, String> = emptyMap()
) {
    fun getTotalWidth(): Double {
        val leftWidth = leftPoints.lastOrNull()?.distanceTo2D(centerLinePoint) ?: 0.0
        val rightWidth = rightPoints.lastOrNull()?.distanceTo2D(centerLinePoint) ?: 0.0
        return leftWidth + rightWidth
    }
}

/**
 * DXF Dosya Formatı
 */
data class DXFFile(
    val version: String = "AC1015",
    val layers: List<CADLayer> = emptyList(),
    val objects: List<CADObject> = emptyList()
)

