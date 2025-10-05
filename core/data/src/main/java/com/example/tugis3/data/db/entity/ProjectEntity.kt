package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val description: String? = null,
    val ellipsoidName: String? = null,
    val semiMajorA: Double? = null,
    val invFlattening: Double? = null,
    // UTM projeksiyon parametreleri (opsiyonel)
    val utmZone: Int? = null,
    val utmNorthHemisphere: Boolean = true,
    val epsgCode: Int? = null,

    // Gelişmiş projeksiyon parametreleri (opsiyonel) - farklı projeksiyon türleri için
    val projectionType: String? = null, // Örn: "Transverse_Mercator", "Lambert_Conformal_Conic_2SP"
    val projCentralMeridianDeg: Double? = null,
    val projFalseNorthing: Double? = null,
    val projFalseEasting: Double? = null,
    val projScaleFactor: Double? = null,
    val projLatOrigin: Double? = null,
    val projStdParallel1: Double? = null,
    val projStdParallel2: Double? = null,

    // --- Lokalizasyon (Similarity Transform) Parametreleri ---
    // Null ise lokalizasyon uygulanmaz
    val locScale: Double? = null,
    val locRotRad: Double? = null, // rotation (radyan)
    val locTx: Double? = null,
    val locTy: Double? = null,
    val locPointCount: Int? = null, // çözümde kullanılan nokta sayısı
    val locLastSolvedAt: Long? = null
)
