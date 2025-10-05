package com.example.tugis3.core.cad.geom

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import com.example.tugis3.core.cad.model.*

/**
 * Geometri hesaplamaları için yardımcı fonksiyonlar.
 * Not: Tüm uzunluk/alan sonuçları model koordinat sistemine göredir (projeksiyon dışı).
 */
object GeometryUtils {
    // --- Temel Nokta & Vektör ---
    fun distance(a: Vec2, b: Vec2): Double {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx*dx + dy*dy)
    }

    // --- Polyline ---
    fun polylineLength(points: List<Vec2>): Double {
        if (points.size < 2) return 0.0
        var d = 0.0
        for (i in 1 until points.size) d += distance(points[i-1], points[i])
        return d
    }

    // --- Polygon ---
    /** Shoelace algoritması (signed alan). Son nokta kapatılmış varsayılır (ek olarak tekrar eklemeye gerek yok). */
    fun polygonAreaSigned(points: List<Vec2>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += (a.x * b.y - b.x * a.y)
        }
        return 0.5 * sum
    }

    fun polygonArea(points: List<Vec2>): Double = abs(polygonAreaSigned(points))

    /** Çok halkalı (holes) poligon alanı. İlk ring dış, diğerleri iç boşluk (çıkarılır). */
    fun polygonAreaRings(rings: List<List<Vec2>>): Double {
        if (rings.isEmpty()) return 0.0
        var area = polygonArea(rings[0])
        if (rings.size > 1) {
            for (i in 1 until rings.size) area -= polygonArea(rings[i])
        }
        return max(0.0, area)
    }

    // --- Circle / Arc ---
    fun circleArea(r: Double) = PI * r * r
    fun circleCircumference(r: Double) = 2 * PI * r

    /** Arc süpürme açısı (pozitif). */
    fun arcSweepDeg(startDeg: Double, endDeg: Double): Double {
        var sweep = endDeg - startDeg
        if (sweep < 0) sweep += 360.0
        return sweep
    }

    fun arcLength(r: Double, startDeg: Double, endDeg: Double): Double = r * Math.toRadians(arcSweepDeg(startDeg, endDeg))

    // --- BoundingBox Yardımcıları ---
    /** Arc için kaba (tam daire) bounding box – ileride açısal kesişim kontrolü ile optimize edilebilir. */
    fun arcBoundingBox(center: Vec2, radius: Double): BoundingBox = BoundingBox(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
    )

    // --- CadEntity Extension ---
    fun CadEntity.lengthOrPerimeter(): Double = when(this) {
        is CadLine -> distance(start, end)
        is CadPolyline -> polylineLength(points) + if (isClosed && points.size > 2) distance(points.last(), points.first()) else 0.0
        is CadCircle -> circleCircumference(radius)
        is CadArc -> arcLength(radius, startAngleDeg, endAngleDeg)
        is CadPolygon -> polygonPerimeterRings(rings)
        is CadPoint, is CadText -> 0.0
    }

    fun CadEntity.area(): Double = when(this) {
        is CadPolygon -> polygonAreaRings(rings)
        is CadCircle -> circleArea(radius)
        // Arc, polyline, line, point, text alanı 0 kabul
        else -> 0.0
    }

    // --- Perimeter ---
    fun polygonPerimeter(points: List<Vec2>): Double {
        if (points.size < 2) return 0.0
        var d = 0.0
        for (i in 1 until points.size) d += distance(points[i-1], points[i])
        // kapat
        d += distance(points.last(), points.first())
        return d
    }

    fun polygonPerimeterRings(rings: List<List<Vec2>>): Double {
        if (rings.isEmpty()) return 0.0
        var sum = 0.0
        rings.forEach { if (it.size >= 2) sum += polygonPerimeter(it) }
        return sum
    }

    fun polygonOuterPerimeter(rings: List<List<Vec2>>): Double = if (rings.isEmpty()) 0.0 else polygonPerimeter(rings.first())
}

// Kısa alias erişimleri
fun polylineLength(points: List<Vec2>) = GeometryUtils.polylineLength(points)
fun polygonArea(points: List<Vec2>) = GeometryUtils.polygonArea(points)
fun circleArea(r: Double) = GeometryUtils.circleArea(r)
fun circleCircumference(r: Double) = GeometryUtils.circleCircumference(r)
fun arcLength(r: Double, startDeg: Double, endDeg: Double) = GeometryUtils.arcLength(r, startDeg, endDeg)
fun polygonPerimeter(points: List<Vec2>) = GeometryUtils.polygonPerimeter(points)
fun polygonPerimeterRings(rings: List<List<Vec2>>) = GeometryUtils.polygonPerimeterRings(rings)
fun polygonOuterPerimeter(rings: List<List<Vec2>>) = GeometryUtils.polygonOuterPerimeter(rings)
fun polygonAreaRings(rings: List<List<Vec2>>) = GeometryUtils.polygonAreaRings(rings)
