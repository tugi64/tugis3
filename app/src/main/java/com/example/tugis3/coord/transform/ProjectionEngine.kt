package com.example.tugis3.coord.transform

import com.example.tugis3.data.db.entity.ProjectEntity
import kotlin.math.*

/** Ortak arayüz: Jeodezik (lat,lon) -> düz koordinat (x,y) dönüşümü ve tersi */
interface CoordinateTransformer {
    fun forward(latDeg: Double, lonDeg: Double): Pair<Double, Double>
    fun inverse(x: Double, y: Double): Pair<Double, Double>
}

/** Hiçbir dönüşüm yapmayan (lat->northing, lon->easting) placeholder. */
object NoOpTransformer : CoordinateTransformer {
    override fun forward(latDeg: Double, lonDeg: Double) = latDeg to lonDeg
    override fun inverse(x: Double, y: Double) = x to y
}

/** UTM iskeleti – Henüz gerçek matematik yok. TODO: Transverse Mercator formülleri. */
class UtmTransformer(
    private val zone: Int,
    private val northernHemisphere: Boolean,
    private val semiMajor: Double,
    private val invF: Double
) : CoordinateTransformer {
    private val f = 1.0 / invF
    private val e2 = 2*f - f*f
    private val ePrime2 = e2 / (1 - e2)
    private val k0 = 0.9996
    private val falseEasting = 500000.0
    private val falseNorthing = if (northernHemisphere) 0.0 else 10000000.0
    private val lambda0 = Math.toRadians((zone * 6 - 183).toDouble())

    override fun forward(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)
        val N = semiMajor / sqrt(1 - e2 * sin(lat).pow(2))
        val T = tan(lat).pow(2)
        val C = ePrime2 * cos(lat).pow(2)
        val A = cos(lat) * (lon - lambda0)
        val M = semiMajor * (
            (1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256) * lat -
                (3*e2/8 + 3*e2*e2/32 + 45*e2*e2*e2/1024) * sin(2*lat) +
                (15*e2*e2/256 + 45*e2*e2*e2/1024) * sin(4*lat) -
                (35*e2*e2*e2/3072) * sin(6*lat)
        )
        val x = k0 * N * (A + (1 - T + C) * A.pow(3)/6 + (5 - 18*T + T*T + 72*C - 58*ePrime2) * A.pow(5)/120) + falseEasting
        val y = k0 * (M + N * tan(lat) * (A*A/2 + (5 - T + 9*C + 4*C*C) * A.pow(4)/24 + (61 - 58*T + T*T + 600*C - 330*ePrime2) * A.pow(6)/720)) + falseNorthing
        return x to y
    }
    override fun inverse(x: Double, y: Double): Pair<Double, Double> {
        val xAdj = x - falseEasting
        val yAdj = y - falseNorthing
        val M = yAdj / k0
        val mu = M / (semiMajor * (1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256))
        val e1 = (1 - sqrt(1 - e2)) / (1 + sqrt(1 - e2))
        val J1 = (3*e1/2 - 27*e1.pow(3)/32)
        val J2 = (21*e1*e1/16 - 55*e1.pow(4)/32)
        val J3 = (151*e1.pow(3)/96)
        val J4 = (1097*e1.pow(4)/512)
        val fp = mu + J1*sin(2*mu) + J2*sin(4*mu) + J3*sin(6*mu) + J4*sin(8*mu)
        val C1 = ePrime2 * cos(fp).pow(2)
        val T1 = tan(fp).pow(2)
        val N1 = semiMajor / sqrt(1 - e2 * sin(fp).pow(2))
        val R1 = N1 * (1 - e2) / (1 - e2 * sin(fp).pow(2))
        val D = xAdj / (N1 * k0)
        val lat = fp - (N1 * tan(fp) / R1) * (D*D/2 - (5 + 3*T1 + 10*C1 - 4*C1*C1 - 9*ePrime2) * D.pow(4)/24 + (61 + 90*T1 + 298*C1 + 45*T1*T1 - 252*ePrime2 - 3*C1*C1) * D.pow(6)/720)
        val lon = lambda0 + (D - (1 + 2*T1 + C1) * D.pow(3)/6 + (5 - 2*C1 + 28*T1 - 3*C1*C1 + 8*ePrime2 + 24*T1*T1) * D.pow(5)/120) / cos(fp)
        return Math.toDegrees(lat) to Math.toDegrees(lon)
    }
}

/** Genel parametreli Transverse Mercator (UTM genelleştirilmiş) */
private class GenericTmTransformer(
    private val semiMajor: Double,
    invF: Double,
    private val centralMeridianDeg: Double,
    private val latOriginDeg: Double,
    private val scaleFactor: Double,
    private val falseE: Double,
    private val falseN: Double
) : CoordinateTransformer {
    private val f = 1.0 / invF
    private val e2 = 2*f - f*f
    private val ePrime2 = e2 / (1 - e2)
    private val lambda0 = Math.toRadians(centralMeridianDeg)
    private val phi0 = Math.toRadians(latOriginDeg)
    // Meridyen yayı için yardımcı katsayılar
    private val a0 = 1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256
    private val a2 = 3.0/8.0 * (e2 + e2*e2/4 + 15*e2*e2*e2/128)
    private val a4 = 15.0/256.0 * (e2*e2 + 3*e2*e2*e2/4)
    private val a6 = 35.0/3072.0 * e2*e2*e2
    private fun meridianArc(phi: Double): Double = semiMajor * (a0*phi - a2*sin(2*phi) + a4*sin(4*phi) - a6*sin(6*phi))

    override fun forward(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)
        val N = semiMajor / sqrt(1 - e2 * sin(lat).pow(2))
        val T = tan(lat).pow(2)
        val C = ePrime2 * cos(lat).pow(2)
        val A = cos(lat) * (lon - lambda0)
        val M = meridianArc(lat) - meridianArc(phi0)
        val x = falseE + scaleFactor * N * (A + (1 - T + C) * A.pow(3)/6 + (5 - 18*T + T*T + 72*C - 58*ePrime2) * A.pow(5)/120)
        val y = falseN + scaleFactor * (M + N * tan(lat) * (A*A/2 + (5 - T + 9*C + 4*C*C) * A.pow(4)/24 + (61 - 58*T + T*T + 600*C - 330*ePrime2) * A.pow(6)/720))
        return x to y
    }

    override fun inverse(x: Double, y: Double): Pair<Double, Double> {
        val xAdj = (x - falseE) / scaleFactor
        val yAdj = (y - falseN) / scaleFactor + meridianArc(phi0)
        val e1 = (1 - sqrt(1 - e2)) / (1 + sqrt(1 - e2))
        val mu = yAdj / (semiMajor * (1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256))
        val J1 = (3*e1/2 - 27*e1.pow(3)/32)
        val J2 = (21*e1*e1/16 - 55*e1.pow(4)/32)
        val J3 = (151*e1.pow(3)/96)
        val J4 = (1097*e1.pow(4)/512)
        val fp = mu + J1*sin(2*mu) + J2*sin(4*mu) + J3*sin(6*mu) + J4*sin(8*mu)
        val C1 = ePrime2 * cos(fp).pow(2)
        val T1 = tan(fp).pow(2)
        val N1 = semiMajor / sqrt(1 - e2 * sin(fp).pow(2))
        val R1 = N1 * (1 - e2) / (1 - e2 * sin(fp).pow(2))
        val D = xAdj / N1
        val lat = fp - (N1 * tan(fp) / R1) * (D*D/2 - (5 + 3*T1 + 10*C1 - 4*C1*C1 - 9*ePrime2) * D.pow(4)/24 + (61 + 90*T1 + 298*C1 + 45*T1*T1 - 252*ePrime2 - 3*C1*C1) * D.pow(6)/720)
        val lon = lambda0 + (D - (1 + 2*T1 + C1) * D.pow(3)/6 + (5 - 2*C1 + 28*T1 - 3*C1*C1 + 8*ePrime2 + 24*T1*T1) * D.pow(5)/120) / cos(fp)
        return Math.toDegrees(lat) to Math.toDegrees(lon)
    }
}

/** Lambert Conformal Conic 2SP */
private class LambertConic2SPTransformer(
    private val semiMajor: Double,
    invF: Double,
    private val lat0Deg: Double,
    private val lon0Deg: Double,
    private val lat1Deg: Double,
    private val lat2Deg: Double,
    private val falseE: Double,
    private val falseN: Double
): CoordinateTransformer {
    private val f = 1.0 / invF
    private val e2 = 2*f - f*f
    private val e = sqrt(e2)
    private val φ1 = Math.toRadians(lat1Deg)
    private val φ2 = Math.toRadians(lat2Deg)
    private val φ0 = Math.toRadians(lat0Deg)
    private val λ0 = Math.toRadians(lon0Deg)
    private fun m(phi: Double) = cos(phi)/sqrt(1 - e2*sin(phi).pow(2))
    private fun t(phi: Double): Double {
        val esin = e * sin(phi)
        return tan(Math.PI/4 - phi/2) / ((1 - esin)/(1 + esin)).pow(e/2)
    }
    private val m1 = m(φ1)
    private val m2 = m(φ2)
    private val t1 = t(φ1)
    private val t2 = t(φ2)
    private val t0 = t(φ0)
    private val n = (ln(m1) - ln(m2)) / (ln(t1) - ln(t2))
    private val F = m1 / (n * t1.pow(n))
    private val ρ0 = semiMajor * F * t0.pow(n)

    override fun forward(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val φ = Math.toRadians(latDeg)
        val λ = Math.toRadians(lonDeg)
        val tφ = t(φ)
        val ρ = semiMajor * F * tφ.pow(n)
        val θ = n * (λ - λ0)
        val E = falseE + ρ * sin(θ)
        val N = falseN + ρ0 - ρ * cos(θ)
        return E to N
    }

    override fun inverse(x: Double, y: Double): Pair<Double, Double> {
        val dx = x - falseE
        val dy = ρ0 - (y - falseN)
        val ρp = sqrt(dx*dx + dy*dy) * (if (n >= 0) 1 else -1)
        val θ = atan2(dx, dy)
        val tVal = (ρp / (semiMajor * F)).pow(1.0 / n)
        // Iteratif phi çözümü
        var φ = Math.PI/2 - 2*atan(tVal)
        repeat(6) {
            val esin = e * sin(φ)
            φ = Math.PI/2 - 2*atan( tVal * ((1 - esin)/(1 + esin)).pow(e/2) )
        }
        val λ = λ0 + θ / n
        return Math.toDegrees(φ) to Math.toDegrees(λ)
    }
}

object ProjectionEngine {
    /** İleride projeye göre seçilen projeksiyon tipini dönecek. */
    fun forProject(project: ProjectEntity?): CoordinateTransformer {
        if (project == null) return NoOpTransformer
        val a = project.semiMajorA
        val invF = project.invFlattening
        var base: CoordinateTransformer? = null
        if (a != null && invF != null) {
            // Önce gelişmiş tipler
            when(project.projectionType) {
                "Transverse_Mercator" -> {
                    val cm = project.projCentralMeridianDeg
                    if (cm != null) {
                        base = GenericTmTransformer(
                            semiMajor = a,
                            invF = invF,
                            centralMeridianDeg = cm,
                            latOriginDeg = project.projLatOrigin ?: 0.0,
                            scaleFactor = project.projScaleFactor ?: 1.0,
                            falseE = project.projFalseEasting ?: 0.0,
                            falseN = project.projFalseNorthing ?: 0.0
                        )
                    }
                }
                "Lambert_Conformal_Conic_2SP" -> {
                    val cm = project.projCentralMeridianDeg
                    val lat0 = project.projLatOrigin
                    val sp1 = project.projStdParallel1
                    val sp2 = project.projStdParallel2
                    if (cm != null && lat0 != null && sp1 != null && sp2 != null) {
                        base = LambertConic2SPTransformer(
                            semiMajor = a,
                            invF = invF,
                            lat0Deg = lat0,
                            lon0Deg = cm,
                            lat1Deg = sp1,
                            lat2Deg = sp2,
                            falseE = project.projFalseEasting ?: 0.0,
                            falseN = project.projFalseNorthing ?: 0.0
                        )
                    }
                }
            }
            // Gelişmiş tip yoksa (veya parametre eksikse) UTM kullan
            if (base == null) {
                val zone = project.utmZone
                if (zone != null) base = UtmTransformer(zone, project.utmNorthHemisphere, a, invF)
            }
        }
        if (base == null) base = NoOpTransformer
        val scale = project.locScale
        val rot = project.locRotRad
        val tx = project.locTx
        val ty = project.locTy
        return if (scale != null && rot != null && tx != null && ty != null) LocalizedTransformer(base, scale, rot, tx, ty) else base
    }
}

/** Similarity (scale+rotation+translation) uygulayan sarmalayıcı */
private class LocalizedTransformer(
    private val delegate: CoordinateTransformer,
    private val scale: Double,
    private val rot: Double,
    private val tx: Double,
    private val ty: Double
) : CoordinateTransformer {
    private val cosR = cos(rot)
    private val sinR = sin(rot)

    override fun forward(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val (x0, y0) = delegate.forward(latDeg, lonDeg)
        // delegate.forward -> (easting, northing) varsayımı ile
        val e = x0
        val n = y0
        val eL = scale * (cosR * e - sinR * n) + tx
        val nL = scale * (sinR * e + cosR * n) + ty
        return eL to nL
    }

    override fun inverse(x: Double, y: Double): Pair<Double, Double> {
        // Ters similarity: önce translasyonu çıkar, sonra rotasyonu ters çevirip scale'i böl
        val eL = x - tx
        val nL = y - ty
        val invScale = 1.0 / scale
        // R^T * [eL, nL]
        val e = invScale * ( cosR * eL + sinR * nL )
        val n = invScale * ( -sinR * eL + cosR * nL )
        return delegate.inverse(e, n)
    }
}
