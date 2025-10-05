package com.example.tugis3.prefs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefsRepositoryTest {
    @Test
    fun stakeoutTolerance_persist_roundTrip() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = PrefsRepository(ctx)
        repo.setStakeHorizontalTol(0.42)
        repo.setStakeVerticalTol(0.21)
        assertEquals(0.42, repo.stakeHorizontalTol.first(), 1e-9)
        assertEquals(0.21, repo.stakeVerticalTol.first(), 1e-9)
    }

    @Test
    fun lineParams_persist_roundTrip() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = PrefsRepository(ctx)
        repo.setLineInterval(12.5)
        repo.setLineLatTol(0.33)
        repo.setLineChainTol(0.77)
        repo.setLineName("HAT_X")
        assertEquals(12.5, repo.lineInterval.first(), 1e-9)
        assertEquals(0.33, repo.lineLatTol.first(), 1e-9)
        assertEquals(0.77, repo.lineChainTol.first(), 1e-9)
        assertEquals("HAT_X", repo.lineNameFlow.first())
    }
}

