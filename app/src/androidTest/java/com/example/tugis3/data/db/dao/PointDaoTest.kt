package com.example.tugis3.data.db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.tugis3.data.db.AppDatabase
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basit bir enstrümantasyon testi: In-memory Room DB kullanarak PointDao insert & query doğrular.
 * Hilt entegrasyonu gerektirmediği için doğrudan Room.inMemoryDatabaseBuilder ile kurulur.
 */
@RunWith(AndroidJUnit4::class)
class PointDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PointDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Test ortamında kabul edilebilir
            .build()
        dao = db.pointDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserve_returnsInsertedPoint() = runTest {
        val point = PointEntity(
            projectId = 99L,
            name = "P1",
            northing = 450000.0,
            easting = 650000.0,
            ellipsoidalHeight = 120.5
        )
        val id = dao.upsert(point)
        val list = dao.observePoints(99L).first()
        assertEquals(1, list.size)
        assertEquals(id, list.first().id)
        assertEquals("P1", list.first().name)
    }
}

