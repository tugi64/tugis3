package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CAD Katman Entity
 *
 * SurPad 4.2 Layer Settings Uyumlu
 *
 * @property locked Katman kilitli mi? (1=evet, 0=hayır) - Kilitli katmanlarda seçim yapılamaz
 * @property isWorkLayer Aktif çalışma katmanı mı? (1=evet, 0=hayır) - Sadece bir katman aktif olabilir
 */
@Entity(
    tableName = "cad_layers",
    indices = [Index(value = ["projectId","name"], unique = true)]
)
data class CadLayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val colorIndex: Int? = null,
    val visible: Int = 1,
    val locked: Int = 0,           // Yeni alan
    val isWorkLayer: Int = 0,      // Yeni alan
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
