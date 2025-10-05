package com.example.tugis3.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

/**
 * Hilt grafiğinden PointRepository enjekte edilebildiğini ve temel insert/observe akışının
 * çalıştığını doğrular.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PointRepositoryInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var pointRepository: PointRepository

    @Test
    fun repositoryInjection_andBasicCrud() = runTest {
        hiltRule.inject()
        assertNotNull(pointRepository)

        val projectId = 1234L
        val point = PointEntity(
            projectId = projectId,
            name = "TestPoint",
            northing = 1.0,
            easting = 2.0,
            ellipsoidalHeight = 3.0
        )
        val id = pointRepository.upsert(point)
        val list = pointRepository.observePoints(projectId).first()
        assertEquals(1, list.size)
        assertEquals(id, list.first().id)
        assertEquals("TestPoint", list.first().name)
    }
}

