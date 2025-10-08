package com.example.tugis3.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {
    private val _lat = MutableStateFlow<Double?>(null)
    private val _lon = MutableStateFlow<Double?>(null)
    private val _hasFix = MutableStateFlow(false)

    val latitude: StateFlow<Double?> = _lat
    val longitude: StateFlow<Double?> = _lon
    val hasFix: StateFlow<Boolean> = _hasFix

    fun setLocation(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        _lat.value = lat
        _lon.value = lon
        _hasFix.value = true
    }

    fun setLocation(location: Location?) {
        location ?: return
        setLocation(location.latitude, location.longitude)
    }

    fun clearFix() {
        _hasFix.update { false }
    }
}

