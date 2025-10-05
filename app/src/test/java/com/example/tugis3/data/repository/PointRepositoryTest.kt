package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PointRepositoryTest {

    private class FakePointDao : PointDao {
        private val points = mutableListOf<PointEntity>()
        private val deletedPoints = mutableListOf<PointEntity>()
        private val stateMap = mutableMapOf<Long, MutableStateFlow<List<PointEntity>>>()
        private var idGen = 1L

        override fun observePoints(projectId: Long): Flow<List<PointEntity>> =
            stateMap.getOrPut(projectId) { MutableStateFlow(currentFor(projectId)) }

        override fun observeDeletedPoints(projectId: Long, limit: Int): Flow<List<PointEntity>> =
            flowOf(deletedPoints.filter { it.projectId == projectId }.take(limit))

        override fun searchPoints(projectId: Long, pattern: String): Flow<List<PointEntity>> =
            flowOf(points.filter { it.projectId == projectId && it.name.contains(pattern, ignoreCase = true) })

        private fun currentFor(projectId: Long) = points.filter { it.projectId == projectId }
            .sortedByDescending { it.createdAt }

        private fun updateFlows() = stateMap.forEach { (pid, flow) -> flow.value = currentFor(pid) }

        override suspend fun upsert(point: PointEntity): Long {
            val assigned = if (point.id == 0L) point.copy(id = idGen++) else point
            val idx = points.indexOfFirst { it.id == assigned.id }
            if (idx >= 0) points[idx] = assigned else points.add(assigned)
            updateFlows(); return assigned.id
        }

        override suspend fun delete(point: PointEntity) {
            points.removeIf { it.id == point.id }
            deletedPoints.add(point)
            updateFlows()
        }

        override suspend fun deleteByProject(projectId: Long) {
            val toDelete = points.filter { it.projectId == projectId }
            points.removeIf { it.projectId == projectId }
            deletedPoints.addAll(toDelete)
            updateFlows()
        }

        override suspend fun softDelete(ids: List<Long>, ts: Long) {
            val toDelete = points.filter { it.id in ids }
            points.removeAll(toDelete)
            deletedPoints.addAll(toDelete.map { it.copy(updatedAt = ts) })
            updateFlows()
        }

        override suspend fun restore(ids: List<Long>, ts: Long) {
            val toRestore = deletedPoints.filter { it.id in ids }
            deletedPoints.removeAll(toRestore)
            points.addAll(toRestore.map { it.copy(updatedAt = ts) })
            updateFlows()
        }

        override suspend fun updatePointFull(
            id: Long,
            name: String,
            northing: Double,
            easting: Double,
            height: Double?,
            code: String?,
            desc: String?,
            updatedAt: Long
        ) {
            val idx = points.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val old = points[idx]
                points[idx] = old.copy(
                    name = name,
                    northing = northing,
                    easting = easting,
                    ellipsoidalHeight = height,
                    featureCode = code,
                    description = desc,
                    updatedAt = updatedAt
                )
                updateFlows()
            }
        }

        override suspend fun countName(projectId: Long, name: String, excludeId: Long): Int =
            points.count { it.projectId == projectId && it.name == name && it.id != excludeId }
    }

    @Test
    fun upsertAndObserve_emitsInsertedPoint() = runTest {
        val repo = PointRepository(FakePointDao())
        val p = PointEntity(projectId = 10L, name = "N1", northing = 100.0, easting = 200.0, ellipsoidalHeight = 50.0)
        val id = repo.upsert(p)
        val list = repo.observePoints(10L).first()
        assertEquals(1, list.size)
        assertEquals(id, list.first().id)
        assertEquals("N1", list.first().name)
    }

    @Test
    fun deleteByProject_removesOnlyThatProjectsPoints() = runTest {
        val repo = PointRepository(FakePointDao())
        repeat(3) { repo.upsert(PointEntity(projectId = 1L, name = "P1-$it", northing = 1.0, easting = 2.0, ellipsoidalHeight = 3.0)) }
        repeat(2) { repo.upsert(PointEntity(projectId = 2L, name = "P2-$it", northing = 4.0, easting = 5.0, ellipsoidalHeight = 6.0)) }
        assertEquals(3, repo.observePoints(1L).first().size)
        assertEquals(2, repo.observePoints(2L).first().size)
        repo.deleteByProject(1L)
        assertEquals(0, repo.observePoints(1L).first().size)
        assertEquals(2, repo.observePoints(2L).first().size)
    }

    @Test
    fun updatePointBasic_updatesFieldsAndTimestamp() = runTest {
        val repo = PointRepository(FakePointDao())
        val id = repo.upsert(PointEntity(projectId = 3L, name = "Orig", northing = 10.0, easting = 20.0, ellipsoidalHeight = 1.0))
        val before = repo.observePoints(3L).first().first { it.id == id }
        // kısa bekleme fark yaratması için
        kotlinx.coroutines.delay(5)
        repo.updatePointBasic(id, "NewName", 99.0, 88.0)
        val after = repo.observePoints(3L).first().first { it.id == id }
        assertEquals("NewName", after.name)
        assertEquals(99.0, after.northing, 0.0)
        assertEquals(88.0, after.easting, 0.0)
        assertNotEquals("updatedAt should change", before.updatedAt, after.updatedAt)
    }
}
