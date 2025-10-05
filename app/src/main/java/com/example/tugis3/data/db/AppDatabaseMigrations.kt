package com.example.tugis3.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase migration tanımları.
 */
object AppDatabaseMigrations {
    // 5 -> 6: points tablosuna updatedAt INTEGER NOT NULL DEFAULT 0 sütunu eklenir
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE points ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        }
    }
    val ALL = arrayOf(MIGRATION_5_6)
}

