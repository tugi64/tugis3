package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    tableName = "line_definitions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LineDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "projectId", index = true)
    val projectId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "startPointId")
    val startPointId: Long,

    @ColumnInfo(name = "endPointId")
    val endPointId: Long,

    @ColumnInfo(name = "startKm")
    val startKm: Double = 0.0,

    @ColumnInfo(name = "length")
    val length: Double = 0.0,

    @ColumnInfo(name = "azimuth")
    val azimuth: Double = 0.0,

    @ColumnInfo(name = "useInterval")
    val useInterval: Boolean = false,

    @ColumnInfo(name = "intervalMeters")
    val intervalMeters: Double = 5.0,

    @ColumnInfo(name = "slopePercent")
    val slopePercent: Double = 0.0,

    @ColumnInfo(name = "autoNearestPoint")
    val autoNearestPoint: Boolean = false,

    @ColumnInfo(name = "calculationMode")
    val calculationMode: String = "FIXED_DISTANCE",

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

