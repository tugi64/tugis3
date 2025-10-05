package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "points",
    indices = [
        Index(value = ["projectId"], name = "index_points_projectId"),
        Index(value = ["name"], name = "index_points_name"),
        Index(value = ["projectId", "deleted"], name = "index_points_projectId_deleted")
    ]
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val northing: Double,
    val easting: Double,
    val ellipsoidalHeight: Double?,
    val orthoHeight: Double? = null,
    val latDeg: Double? = null,
    val lonDeg: Double? = null,
    val fixType: String? = null,
    val hrms: Double? = null,
    val pdop: Double? = null,
    val hdop: Double? = null,
    val vdop: Double? = null,
    val featureCode: String? = null,
    val description: String? = null,
    val deleted: Int = 0, // 0 aktif, 1 silinmiş
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
