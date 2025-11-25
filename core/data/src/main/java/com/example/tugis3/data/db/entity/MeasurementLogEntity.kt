package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_logs"
)
data class MeasurementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long?,
    val mode: String?,
    val eventType: String, // ör: EPOCH_DONE, AUTO_POINT, FAST_POINT, GRAPH_OBJ, CAD_IMPORT
    val message: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val extra: String? = null
)
