package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cad_entities",
    indices = [Index(value=["projectId"], name="index_cad_entities_projectId"), Index(value=["layerId"], name="index_cad_entities_layerId")]
)
data class CadEntityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val layerId: Long,
    val type: String,          // L, PL, C, A, T
    val dataEncoded: String,   // compact pipe separated encoding
    val colorIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
