package com.example.tugis3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val coordinateSystem: String,
    val datum: String,
    val projection: String,
    val centralMeridian: Double?,
    val scaleFactor: Double?,
    val falseEasting: Double?,
    val falseNorthing: Double?,
    val createdAt: Long,
    val lastModified: Long,
    val isActive: Boolean = false
)
