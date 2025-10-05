package com.example.tugis3.core.cad.dto

import com.example.tugis3.core.cad.model.*

/** DTO şema sürümü */
const val CAD_DTO_VERSION: Int = 1

/**
 * Ağ / dosya paylaşımı için hafif DTO katmanı.
 * Amaç: Domain modellerini (kotlin sealed interface + double) dışa aktarım / import sırasında
 * sürümleyerek kırılganlığı azaltmak.
 */
sealed interface CadEntityDto { val layer: String }

data class PointDto(val x: Double, val y: Double, override val layer: String): CadEntityDto

data class LineDto(val x1: Double, val y1: Double, val x2: Double, val y2: Double, override val layer: String): CadEntityDto

data class PolylineDto(val closed: Boolean, val coords: List<Double>, override val layer: String): CadEntityDto {
    init { require(coords.size % 2 == 0) { "Koordinat sayısı çift olmalı" } }
}

data class PolygonDto(val rings: List<List<Double>>, override val layer: String): CadEntityDto {
    init { require(rings.isNotEmpty()) { "En az bir ring" } }
}

data class CircleDto(val cx: Double, val cy: Double, val r: Double, override val layer: String): CadEntityDto

data class ArcDto(val cx: Double, val cy: Double, val r: Double, val startDeg: Double, val endDeg: Double, override val layer: String): CadEntityDto

// rotationDeg (rot) eklendi, uyumluluk için default 0.0
data class TextDto(
    val x: Double,
    val y: Double,
    val h: Double,
    val text: String,
    val rot: Double = 0.0,
    override val layer: String
): CadEntityDto
