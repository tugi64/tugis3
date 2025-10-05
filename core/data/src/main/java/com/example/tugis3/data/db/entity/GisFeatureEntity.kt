package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "gis_features",
    indices = [Index(value = ["projectId", "id"])]
)
data class GisFeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String, // Nokta / Çizgi / Alan (şimdilik basit metin)
    val attr: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

