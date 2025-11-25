package com.example.tugis3.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tugis3.data.db.dao.ProjectDao
import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.dao.NtripProfileDao
import com.example.tugis3.data.db.dao.CalibrationPointDao
import com.example.tugis3.data.db.dao.NtripSessionDao
import com.example.tugis3.data.db.dao.CadLayerDao
import com.example.tugis3.data.db.dao.CadEntityDao
import com.example.tugis3.data.db.entity.ProjectEntity
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.db.entity.NtripProfileEntity
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.db.entity.DetailFeatureEntity
import com.example.tugis3.data.db.entity.DetailFeaturePointEntity
import com.example.tugis3.data.db.entity.CalibrationPointEntity
import com.example.tugis3.data.db.entity.NtripSessionEntity
import com.example.tugis3.data.db.entity.CadLayerEntity
import com.example.tugis3.data.db.entity.CadEntityEntity
import com.example.tugis3.data.db.entity.SurveyRangeEntity
import com.example.tugis3.data.db.dao.SurveyRangeDao
import com.example.tugis3.data.db.entity.GisFeatureEntity
import com.example.tugis3.data.db.dao.GisFeatureDao
import com.example.tugis3.data.db.entity.MeasurementLogEntity
import com.example.tugis3.data.db.dao.MeasurementLogDao
import com.example.tugis3.data.db.entity.LineDefinitionEntity
import com.example.tugis3.data.db.dao.LineDefinitionDao

// Dummy reference to enforce symbol resolution (no-op)
@Suppress("unused")
private val _calibRef = CalibrationPointEntity::class

@Database(
    entities = arrayOf(
        ProjectEntity::class,
        PointEntity::class,
        NtripProfileEntity::class,
        SurveyPointEntity::class,
        DetailFeatureEntity::class,
        DetailFeaturePointEntity::class,
        CalibrationPointEntity::class,
        NtripSessionEntity::class,
        CadLayerEntity::class,
        CadEntityEntity::class,
        SurveyRangeEntity::class,
        GisFeatureEntity::class,
        MeasurementLogEntity::class,
        LineDefinitionEntity::class
    ),
    version = 34, // 33 -> 34: cad_layers tablosuna locked ve isWorkLayer alanları eklendi
    exportSchema = false // Geçici: kotlinx-serialization sürüm çakışmasını önlemek için kapatıldı
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun pointDao(): PointDao
    abstract fun ntripProfileDao(): NtripProfileDao
    abstract fun surveyPointDao(): com.example.tugis3.data.db.dao.SurveyPointDao
    abstract fun detailFeatureDao(): com.example.tugis3.data.db.dao.DetailFeatureDao
    abstract fun calibrationPointDao(): CalibrationPointDao
    abstract fun ntripSessionDao(): NtripSessionDao
    abstract fun cadLayerDao(): CadLayerDao
    abstract fun cadEntityDao(): CadEntityDao
    abstract fun surveyRangeDao(): SurveyRangeDao
    abstract fun gisFeatureDao(): GisFeatureDao
    abstract fun measurementLogDao(): MeasurementLogDao
    abstract fun lineDefinitionDao(): LineDefinitionDao

    companion object {
        // MIGRATION_26_27 önce burada tanımlıydı; tekil kaynak için AppDatabaseMigrations.ALL kullanılıyor.
        // Yeni migration zinciri AppDatabaseMigrations içinde yönetilecek.
    }
}
// Room veritabanı oluşturulurken migration eklenmeli:
// Room.databaseBuilder(context, AppDatabase::class.java, "tugis-db")
//     .addMigrations(AppDatabase.MIGRATION_23_24)
//     .build()
