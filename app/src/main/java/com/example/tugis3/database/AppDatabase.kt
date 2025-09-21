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
                    "tugis3_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
