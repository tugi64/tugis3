package com.example.tugis3.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import org.junit.Assert.assertTrue

/**
 * Güncel migration doğrulama testi.
 * Zincir: 5 -> 6 -> 7 -> 8 -> 9 -> 10 -> 11
 * v11: soft delete sütunları (deleted, deletedAt, index)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        AppDatabase::class.java
    )

    private val DB_NAME = "migration-test.db"

    @Test
    @Throws(IOException::class)
    fun migrate5To12_fullChain() {
        // v5 database oluştur (Room master tablo identity hash'i daha sonra override edilecek)
        var db: SupportSQLiteDatabase = helper.createDatabase(DB_NAME, 5)
        // (İsteğe bağlı) örnek tablo ek veri eklenebilir
        db.close()
        // Bütün migration zinciri ile 12'ye geçir ve doğrula
        db = helper.runMigrationsAndValidate(
            DB_NAME,
            12,
            true,
            *AppDatabaseMigrations.ALL
        )
        db.close()
    }

    @Test
    fun migrate10To11_addsSoftDeleteColumns() {
        val name = "migration-softdelete.db"
        // v10 oluştur (deleted sütunları yok)
        var db = helper.createDatabase(name, 10)
        // Örnek veri ekle
        db.execSQL(
            "INSERT INTO points (projectId, name, northing, easting, ellipsoidalHeight, orthoHeight, latDeg, lonDeg, fixType, hrms, pdop, hdop, vdop, featureCode, description, createdAt, updatedAt) " +
                "VALUES (1,'Before',100.0,200.0,5.0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,123456789,123456789)"
        )
        db.close()
        // Migration uygula
        db = helper.runMigrationsAndValidate(
            name,
            11,
            true,
            *AppDatabaseMigrations.ALL
        )
        // deleted / deletedAt var mı kontrol
        val pragma = db.query("PRAGMA table_info(points)")
        var hasDeleted = false
        var hasDeletedAt = false
        val colNameIdx = pragma.getColumnIndex("name")
        while (pragma.moveToNext()) {
            when (pragma.getString(colNameIdx)) {
                "deleted" -> hasDeleted = true
                "deletedAt" -> hasDeletedAt = true
            }
        }
        pragma.close()
        assertTrue("deleted sütunu eklenmemiş", hasDeleted)
        assertTrue("deletedAt sütunu eklenmemiş", hasDeletedAt)
        // Varsayılan değer kontrolü
        db.query("SELECT deleted, deletedAt FROM points WHERE name='Before'").use { c ->
            assertTrue(c.moveToFirst())
            val del = c.getInt(0)
            val delAt = if (c.isNull(1)) null else c.getLong(1)
            assertTrue("deleted varsayılan 0 olmalı", del == 0)
            assertTrue("deletedAt varsayılan NULL olmalı", delAt == null)
        }
        db.close()
    }

    @Test
    fun migrate11To12_addsProjectColumns() {
        val name = "migration-projcols.db"
        // v11 oluştur (utmZone, utmNorthHemisphere, epsgCode sütunları yok)
        var db = helper.createDatabase(name, 11)
        // Eski sürümde yeni sütunlar yok; örnek proje ekleyelim
        db.execSQL("INSERT INTO projects (id,name,createdAt,isActive) VALUES (1,'Test',123456789,0)")
        db.close()
        // Migration uygula
        db = helper.runMigrationsAndValidate(name, 12, true, *AppDatabaseMigrations.ALL)
        // utmZone, utmNorthHemisphere, epsgCode sütunları var mı kontrol et
        db.query("PRAGMA table_info(projects)").use { c ->
            var hasUtmZone=false; var hasHem=false; var hasEpsg=false
            val idx = c.getColumnIndex("name")
            while (c.moveToNext()) {
                when (c.getString(idx)) {
                    "utmZone" -> hasUtmZone = true
                    "utmNorthHemisphere" -> hasHem = true
                    "epsgCode" -> hasEpsg = true
                }
            }
            assertTrue("utmZone eklenmemiş", hasUtmZone)
            assertTrue("utmNorthHemisphere eklenmemiş", hasHem)
            assertTrue("epsgCode eklenmemiş", hasEpsg)
        }
        // Varsayılan değer kontrolü (utmNorthHemisphere = 1)
        db.query("SELECT utmNorthHemisphere FROM projects WHERE id=1").use { c ->
            assertTrue(c.moveToFirst())
            val hem = c.getInt(0)
            assertTrue("utmNorthHemisphere default 1 değil", hem == 1)
        }
        db.close()
    }

    @Test
    fun openFresh8_createsDb() {
        // Doğrudan 8 ile açma (taze kurulum senaryosu)
        val context = instrumentation.targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "fresh-open.db")
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()
        db.openHelper.writableDatabase // tetikle
        db.close()
    }
}
