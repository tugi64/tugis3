package com.example.tugis3.data.repository

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Similarity (2D) çözümü: scale, rot, tx, ty */
object LocalizationSolver {
    data class Point(
        val srcE: Double,
        val srcN: Double,
        val dstE: Double,
        val dstN: Double,
        val w: Double = 1.0
    )
    data class SimilarityParams(val scale: Double, val rot: Double, val tx: Double, val ty: Double)

    /**
     * Weighted 2D similarity transform (Helmert) çözümü.
     * En az 2 nokta gerekir. Degenerate (D ~ 0) durumda hata fırlatır.
     */
    fun solveSimilarity(points: List<Point>): SimilarityParams {
        require(points.size >= 2) { "En az 2 nokta gerekli" }
        var sumW = 0.0
        var sumSrcE = 0.0
        var sumSrcN = 0.0
        var sumDstE = 0.0
        var sumDstN = 0.0
        points.forEach { p ->
            sumW += p.w
            sumSrcE += p.w * p.srcE
            sumSrcN += p.w * p.srcN
            sumDstE += p.w * p.dstE
            sumDstN += p.w * p.dstN
        }
        if (sumW == 0.0) error("Ağırlık toplamı 0")
        val srcEc = sumSrcE / sumW
        val srcNc = sumSrcN / sumW
        val dstEc = sumDstE / sumW
        val dstNc = sumDstN / sumW

        var Sxx = 0.0
        var Sxy = 0.0
        var D = 0.0
        points.forEach { p ->
            val xs = p.srcE - srcEc
            val ys = p.srcN - srcNc
            val xd = p.dstE - dstEc
            val yd = p.dstN - dstNc
            Sxx += p.w * (xs * xd + ys * yd)
            Sxy += p.w * (xs * yd - ys * xd)
            D += p.w * (xs * xs + ys * ys)
        }
        if (D == 0.0) error("Degenerate nokta geometrisi (D=0)")
        val scale = sqrt(Sxx * Sxx + Sxy * Sxy) / D
        val rot = atan2(Sxy, Sxx)
        val cosR = cos(rot)
        val sinR = sin(rot)
        val tx = dstEc - scale * (cosR * srcEc - sinR * srcNc)
        val ty = dstNc - scale * (sinR * srcEc + cosR * srcNc)
        return SimilarityParams(scale, rot, tx, ty)
    }
}
