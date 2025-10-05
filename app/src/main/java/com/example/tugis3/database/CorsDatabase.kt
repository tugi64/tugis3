package com.example.tugis3.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tugis3.data.model.CorsStation
import com.example.tugis3.database.dao.CorsDao

@Database(entities = [CorsStation::class], version = 1, exportSchema = false)
abstract class CorsDatabase : RoomDatabase() {
    abstract fun corsDao(): CorsDao
}

