package com.example.tugis3.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import java.io.IOException

/**
 * Gerçek 5 -> 6 migration testi: v5 şemasında DB oluşturup migration uygulayarak
 * - updatedAt sütununun eklendiğini
 * - Mevcut satırlarda updatedAt = 0 (migration DEFAULT değeri) geldiğini
 * - Yeni eklenen kayıtlarda updatedAt > 0 set edildiğini
 * - updatePointBasic çağrısı ile updatedAt değerinin değiştiğini doğrular.
 */
@RunWith(AndroidJUnit4::class)
class RealMigrationTest {

    private val dbName = "migration-test-db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(), // AutoMigration yok
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6_addsUpdatedAtColumn_andPreservesData() {
        // 1) Eski (v5) veritabanını oluştur ve bir eski kayıt ekle
        helper.createDatabase(dbName, 5).apply {
            execSQL(
                "INSERT INTO points (projectId, name, northing, easting, ellipsoidalHeight, orthoHeight, latDeg, lonDeg, fixType, hrms, pdop, hdop, vdop, featureCode, description, createdAt) " +
                    "VALUES (1, 'Legacy', 10.0, 20.0, 5.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1111111111)"
            )
            close() // Eski sürüm DB kapanır
        }

        // 2) Migration uygulayarak DB'yi aç
        val migratedDb = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName
        ).addMigrations(*AppDatabaseMigrations.ALL)
            .allowMainThreadQueries() // test kolaylığı
            .build()

        // 3) Şemada updatedAt sütunu var mı?
        migratedDb.openHelper.writableDatabase.query("PRAGMA table_info(points)").use { cursor ->
            var hasUpdatedAt = false
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == "updatedAt") {
                    hasUpdatedAt = true
                    break
                }
            }
            assertTrue("updatedAt kolonu migration sonrası bulunmalı", hasUpdatedAt)
        }

        // 4) Mevcut (legacy) kaydın updatedAt = 0 olduğunu doğrula
        migratedDb.openHelper.writableDatabase.query(
            "SELECT updatedAt, name, northing, easting FROM points WHERE name='Legacy'"
        ).use { c ->
            assertTrue("Legacy kaydı bulunamadı", c.moveToFirst())
            val updatedAt = c.getLong(0)
            val name = c.getString(1)
            val northing = c.getDouble(2)
            val easting = c.getDouble(3)
            assertEquals("Legacy", name)
            assertEquals(10.0, northing, 0.0)
            assertEquals(20.0, easting, 0.0)
            assertEquals("Migration DEFAULT updatedAt bekleniyor", 0L, updatedAt)
        }

        // 5) Yeni bir kayıt ekle (DAO üzerinden) -> updatedAt otomatik (System.currentTimeMillis()) > 0 olmalı
        val pointDao = migratedDb.pointDao()
        val newId = runBlocking {
            pointDao.upsert(
                com.example.tugis3.data.db.entity.PointEntity(
                    projectId = 1L,
                    name = "Fresh",
                    northing = 30.0,
                    easting = 40.0,
                    ellipsoidalHeight = 9.0
                )
            )
        }
        val freshAfterInsert = runBlocking { pointDao.observePoints(1L).first().first { it.id == newId } }
        assertTrue("Yeni kaydın updatedAt > 0 olmalı", freshAfterInsert.updatedAt > 0L)

        // 6) updatePointBasic ile güncelle ve updatedAt değişti mi kontrol et
        val repo = com.example.tugis3.data.repository.PointRepository(pointDao)
        val beforeUpdateTs = freshAfterInsert.updatedAt
        Thread.sleep(5) // zaman ayrımı
        runBlocking { repo.updatePointBasic(newId, "FreshRenamed", 300.0, 400.0) }
        val afterUpdate = runBlocking { pointDao.observePoints(1L).first().first { it.id == newId } }
        assertEquals("FreshRenamed", afterUpdate.name)
        assertEquals(300.0, afterUpdate.northing, 0.0)
        assertEquals(400.0, afterUpdate.easting, 0.0)
        assertTrue("updatedAt artmış olmalı", afterUpdate.updatedAt > beforeUpdateTs)

        migratedDb.close()
    }
}
