package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "detail_feature_points",
    indices = [Index(value = ["featureId"], name = "index_detail_feature_points_featureId")]
)
data class DetailFeaturePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureId: Long,
    val x: Double,
    val y: Double,
    val z: Double?,
    val code: String?,
    val orderIndex: Int,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
