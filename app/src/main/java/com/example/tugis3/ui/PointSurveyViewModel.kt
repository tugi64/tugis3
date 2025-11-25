package com.example.tugis3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PointSurveyViewModel @Inject constructor() : ViewModel() {

    private val _points = MutableStateFlow<List<SurveyPoint>>(emptyList())
    val points: StateFlow<List<SurveyPoint>> = _points

    private val _basePoint = MutableStateFlow<SurveyPoint?>(null)
    val basePoint: StateFlow<SurveyPoint?> = _basePoint

    fun savePoint(
        name: String,
        code: String,
        latitude: Double?,
        longitude: Double?,
        elevation: Double?,
        northing: Double?,
        easting: Double?,
        zone: String?,
        hrms: Double?,
        vrms: Double?,
        pdop: Double?,
        satellites: Int?,
        fixType: String?,
        antennaHeight: Double?
    ) {
        viewModelScope.launch {
            val newPoint = SurveyPoint(
                id = System.currentTimeMillis(),
                name = name,
                code = code,
                latitude = latitude,
                longitude = longitude,
                elevation = elevation,
                northing = northing,
                easting = easting,
                zone = zone,
                hrms = hrms,
                vrms = vrms,
                pdop = pdop,
                satellites = satellites,
                fixType = fixType,
                antennaHeight = antennaHeight,
                timestamp = System.currentTimeMillis()
            )

            val currentPoints = _points.value.toMutableList()
            currentPoints.add(newPoint)
            _points.value = currentPoints
        }
    }

    fun saveBasePoint(
        name: String,
        latitude: Double?,
        longitude: Double?,
        elevation: Double?,
        northing: Double?,
        easting: Double?,
        zone: String?,
        antennaHeight: Double?
    ) {
        viewModelScope.launch {
            val basePoint = SurveyPoint(
                id = System.currentTimeMillis(),
                name = name,
                code = "BASE",
                latitude = latitude,
                longitude = longitude,
                elevation = elevation,
                northing = northing,
                easting = easting,
                zone = zone,
                antennaHeight = antennaHeight,
                timestamp = System.currentTimeMillis(),
                isBasePoint = true
            )

            _basePoint.value = basePoint
        }
    }

    fun deletePoint(pointId: Long) {
        viewModelScope.launch {
            val currentPoints = _points.value.toMutableList()
            currentPoints.removeAll { it.id == pointId }
            _points.value = currentPoints
        }
    }

    fun exportPoints(format: String): String {
        return when (format) {
            "CSV" -> exportToCsv()
            "KML" -> exportToKml()
            "GPX" -> exportToGpx()
            else -> exportToCsv()
        }
    }

    private fun exportToCsv(): String {
        val csv = StringBuilder()
        csv.append("Name,Code,Latitude,Longitude,Elevation,Northing,Easting,Zone,HRMS,VRMS,PDOP,Satellites,FixType,AntennaHeight\n")

        _points.value.forEach { point ->
            csv.append("${point.name},${point.code},${point.latitude},${point.longitude},${point.elevation},${point.northing},${point.easting},${point.zone},${point.hrms},${point.vrms},${point.pdop},${point.satellites},${point.fixType},${point.antennaHeight}\n")
        }

        return csv.toString()
    }

    private fun exportToKml(): String {
        val kml = StringBuilder()
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        kml.append("<Document>\n")
        kml.append("<name>Survey Points</name>\n")

        _points.value.forEach { point ->
            if (point.latitude != null && point.longitude != null) {
                kml.append("<Placemark>\n")
                kml.append("<name>${point.name}</name>\n")
                kml.append("<description>Code: ${point.code}, Elevation: ${point.elevation}</description>\n")
                kml.append("<Point>\n")
                kml.append("<coordinates>${point.longitude},${point.latitude},${point.elevation ?: 0}</coordinates>\n")
                kml.append("</Point>\n")
                kml.append("</Placemark>\n")
            }
        }

        kml.append("</Document>\n")
        kml.append("</kml>")

        return kml.toString()
    }

    private fun exportToGpx(): String {
        val gpx = StringBuilder()
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpx.append("<gpx version=\"1.1\" creator=\"TUGIS3\">\n")

        _points.value.forEach { point ->
            if (point.latitude != null && point.longitude != null) {
                gpx.append("<wpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                point.elevation?.let { gpx.append("<ele>$it</ele>\n") }
                gpx.append("<name>${point.name}</name>\n")
                gpx.append("<desc>${point.code}</desc>\n")
                gpx.append("</wpt>\n")
            }
        }

        gpx.append("</gpx>")

        return gpx.toString()
    }
}

data class SurveyPoint(
    val id: Long,
    val name: String,
    val code: String,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?,
    val northing: Double?,
    val easting: Double?,
    val zone: String?,
    val hrms: Double? = null,
    val vrms: Double? = null,
    val pdop: Double? = null,
    val satellites: Int? = null,
    val fixType: String? = null,
    val antennaHeight: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isBasePoint: Boolean = false
)
