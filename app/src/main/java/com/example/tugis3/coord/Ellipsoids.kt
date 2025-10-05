package com.example.tugis3.coord

/**
 * Ellipsoit modeli: adı, yarı büyük eksen (a) ve düzleştirme tersliği (1/f).
 * description: opsiyonel ek bilgi.
 */
data class Ellipsoid(
    val name: String,
    val semiMajorA: Double,
    val invFlattening: Double,
    val description: String? = null
)

/**
 * Yaygın kullanılan elipsoit parametrelerini listeler.
 */
object Ellipsoids {
    val all: List<Ellipsoid> = listOf(
        Ellipsoid(
            name = "WGS84",
            semiMajorA = 6378137.0,
            invFlattening = 298.257223563,
            description = "World Geodetic System 1984"
        ),
        Ellipsoid(
            name = "GRS80",
            semiMajorA = 6378137.0,
            invFlattening = 298.257222101,
            description = "Geodetic Reference System 1980"
        ),
        Ellipsoid(
            name = "WGS72",
            semiMajorA = 6378135.0,
            invFlattening = 298.26,
            description = "World Geodetic System 1972"
        ),
        Ellipsoid(
            name = "International1924",
            semiMajorA = 6378388.0,
            invFlattening = 297.0,
            description = "Hayford / International 1924"
        ),
        Ellipsoid(
            name = "ED50",
            semiMajorA = 6378388.0,
            invFlattening = 297.0,
            description = "European Datum 1950 (International1924)"
        ),
        Ellipsoid(
            name = "Bessel1841",
            semiMajorA = 6377397.155,
            invFlattening = 299.1528128,
            description = "Bessel 1841"
        ),
        Ellipsoid(
            name = "Clarke1866",
            semiMajorA = 6378206.4,
            invFlattening = 294.9786982,
            description = "Clarke 1866"
        ),
        Ellipsoid(
            name = "Airy1830",
            semiMajorA = 6377563.396,
            invFlattening = 299.3249646,
            description = "Airy 1830"
        ),
        Ellipsoid(
            name = "Helmert1906",
            semiMajorA = 6378200.0,
            invFlattening = 298.3,
            description = "Helmert 1906"
        ),
        Ellipsoid(
            name = "Sphere6371",
            semiMajorA = 6371000.0,
            invFlattening = Double.POSITIVE_INFINITY,
            description = "Spherical approx Earth"
        )
    )
}
