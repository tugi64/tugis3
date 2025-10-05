package com.example.tugis3.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.tugis3.data.model.CorsStation
import com.example.tugis3.data.model.GnssData
import com.example.tugis3.data.model.Project
import com.example.tugis3.database.dao.CorsDao
import com.example.tugis3.database.dao.GnssDataDao
import com.example.tugis3.database.dao.ProjectDao

/**
 * Legacy / ikincil veritabanı.
 * Uygulamanın ana (merkezi) Room DB'si artık core/data modülündeki
 * com.example.tugis3.data.db.AppDatabase sınıfıdır (version 8, migration zinciri ile).
 * Bu sınıf ileride kaldırılabilir; şimdilik geriye dönük kodu bozmayalım diye tutuluyor.
 */
@Deprecated("Merkezi Room DB core/data AppDatabase üzerinden kullanılmalı. Bu sınıf ileride kaldırılacak.")
@Database(
    entities = [
        CorsStation::class,
        GnssData::class,
        Project::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun corsDao(): CorsDao
    abstract fun gnssDataDao(): GnssDataDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    // İsim çakışmasını önlemek için legacy adı
                    "legacy_tugis3_database"
                ).fallbackToDestructiveMigration() // legacy olduğundan agresif yaklaşım kabul edilebilir
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
