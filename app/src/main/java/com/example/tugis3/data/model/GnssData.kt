package com.example.tugis3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gnss_data")
data class GnssData(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val pointName: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val accuracy: Float,
    val fixType: String, // FIXED, FLOAT, SINGLE, etc.
    val satelliteCount: Int,
    val hdop: Float,
    val vdop: Float,
    val pdop: Float,
    val timestamp: Long,
    val isCorsCorrected: Boolean = false,
    val corsStationId: String? = null,
    val rawData: String? = null // NMEA or RINEX data
)
