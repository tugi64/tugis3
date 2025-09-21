package com.example.tugis3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cors_stations")
data class CorsStation(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val mountPoint: String,
    val ntripUrl: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val isActive: Boolean = false,
    val lastConnected: Long? = null,
    val signalQuality: Int = 0
)
