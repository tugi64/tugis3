package com.example.tugis3.core.cad.geo

import kotlin.math.*

/**
 * Çok kaba bir yerel projeksiyon: Orijin olarak ilk referans noktası alınır,
 * enlem-boylam farklarını metreye çevirmek için enlem bazlı ölçek kullanılır.
 * Gerçek projeksiyon (UTM / TM) entegrasyonu ileride eklenecek.
 */
class SimpleLocalProjector(private val originLat: Double, private val originLon: Double) {
    private val meterPerDegLat = 111_320.0 // yaklaşık
    private val meterPerDegLonAtOrigin: Double = 111_320.0 * cos(Math.toRadians(originLat))

    fun toLocal(lat: Double, lon: Double): Pair<Double, Double> {
        val dy = (lat - originLat) * meterPerDegLat
        val dx = (lon - originLon) * meterPerDegLonAtOrigin
        return dx to dy
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val (x1, y1) = toLocal(lat1, lon1)
        val (x2, y2) = toLocal(lat2, lon2)
        return hypot(x2 - x1, y2 - y1)
    }
}

