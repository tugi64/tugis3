package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "survey_ranges",
    indices = [
        Index(value = ["projectId"], name = "index_survey_ranges_projectId"),
        Index(value = ["projectId","name"], unique = true)
    ]
)
data class SurveyRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val pointCount: Int = 0,
    val area: Double? = null, // m^2
    val perimeter: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

