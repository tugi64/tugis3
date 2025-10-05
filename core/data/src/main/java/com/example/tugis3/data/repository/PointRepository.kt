package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.PointDao
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointRepository @Inject constructor(
    private val pointDao: PointDao
) {
    fun observePoints(projectId: Long): Flow<List<PointEntity>> = pointDao.observePoints(projectId)
    fun observeDeletedPoints(projectId: Long, limit: Int = 20): Flow<List<PointEntity>> = pointDao.observeDeletedPoints(projectId, limit)
    fun searchPoints(projectId: Long, pattern: String): Flow<List<PointEntity>> = pointDao.searchPoints(projectId, pattern)

    suspend fun upsert(point: PointEntity): Long = pointDao.upsert(point.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateFull(id: Long, name: String, northing: Double, easting: Double, height: Double?, code: String?, desc: String?) =
        pointDao.updatePointFull(id, name, northing, easting, height, code, desc, System.currentTimeMillis())

    suspend fun updatePointBasic(id: Long, name: String, northing: Double, easting: Double) =
        updateFull(id, name, northing, easting, null, null, null)

    suspend fun softDelete(ids: List<Long>) = pointDao.softDelete(ids, System.currentTimeMillis())
    suspend fun restore(ids: List<Long>) = pointDao.restore(ids, System.currentTimeMillis())

    suspend fun isNameTaken(projectId: Long, name: String, excludeId: Long = 0): Boolean =
        pointDao.countName(projectId, name, excludeId) > 0

    suspend fun delete(point: PointEntity) = pointDao.delete(point) // (tam silme gerekirse)
    suspend fun deleteByProject(projectId: Long) = pointDao.deleteByProject(projectId)
}
