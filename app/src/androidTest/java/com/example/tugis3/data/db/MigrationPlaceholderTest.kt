package com.example.tugis3.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basit şema doğrulama: v6 veritabanında updatedAt sütunu mevcut ve yeni kayıtlarda set ediliyor.
 */
@RunWith(AndroidJUnit4::class)
class MigrationPlaceholderTest {

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    @Test
    fun pointsTable_hasUpdatedAtColumn() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()
        val sqlDb = db.openHelper.writableDatabase
        assertTrue(columnExists(sqlDb, "points", "updatedAt"))

        val pointDao = db.pointDao()
        val id = pointDao.upsert(
            com.example.tugis3.data.db.entity.PointEntity(
                projectId = 1L,
                name = "P1",
                northing = 10.0,
                easting = 20.0,
                ellipsoidalHeight = 5.0
            )
        )
        val list = pointDao.observePoints(1L).first()
        val inserted = list.first { it.id == id }
        assertTrue(inserted.updatedAt >= inserted.createdAt)
        db.close()
    }
}
