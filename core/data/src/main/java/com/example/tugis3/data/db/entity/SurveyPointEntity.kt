package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "survey_points",
    indices = [
        Index(value = ["projectId"], name = "index_survey_points_projectId")
    ]
)
data class SurveyPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val code: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?,
    val northing: Double?,
    val easting: Double?,
    val zone: String?,
    val hrms: Double?,
    val vrms: Double?,
    val pdop: Double?,
    val satellites: Int?,
    val fixType: String?,
    val antennaHeight: Double?,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
