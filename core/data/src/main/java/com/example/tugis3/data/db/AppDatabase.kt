package com.example.tugis3.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        MeasurementLogEntity::class // yeni
    ),
    version = 27, // 26 -> 27 measurement_logs tablosu
    exportSchema = true
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

    companion object {
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tabloyu yeniden oluşturmak için (geliştirme için):
                database.execSQL("DROP TABLE IF EXISTS measurement_logs")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS measurement_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER,
                        mode TEXT,
                        eventType TEXT NOT NULL,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        extra TEXT
                    )"""
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_createdAt ON measurement_logs(createdAt ASC)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_measurement_logs_projectId ON measurement_logs(projectId ASC)")
            }
        }
    }
}
// Room veritabanı oluşturulurken migration eklenmeli:
// Room.databaseBuilder(context, AppDatabase::class.java, "tugis-db")
//     .addMigrations(AppDatabase.MIGRATION_23_24)
//     .build()
