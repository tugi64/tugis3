package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calibration_points",
    indices = [Index(value = ["projectId"], name = "index_calibration_points_projectId")]
)
data class CalibrationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val srcNorth: Double, // Kaynak (ölçülen) northing
    val srcEast: Double,  // Kaynak (ölçülen) easting
    val dstNorth: Double, // Hedef (kontrol) northing
    val dstEast: Double,  // Hedef (kontrol) easting
    val weight: Double = 1.0,
    val include: Int = 1, // 1= dahil, 0= hariç
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
