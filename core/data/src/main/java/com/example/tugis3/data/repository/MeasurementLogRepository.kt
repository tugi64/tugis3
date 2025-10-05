package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.MeasurementLogDao
import com.example.tugis3.data.db.entity.MeasurementLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementLogRepository @Inject constructor(
    private val dao: MeasurementLogDao
) {
    suspend fun log(projectId: Long?, mode: String?, eventType: String, message: String?, extra: String? = null) =
        withContext(Dispatchers.IO) {
            dao.insert(
                MeasurementLogEntity(
                    projectId = projectId,
                    mode = mode,
                    eventType = eventType,
                    message = message,
                    extra = extra
                )
            )
        }

    fun observeRecent(limit: Int = 200): Flow<List<MeasurementLogEntity>> = dao.observeRecent(limit)
    fun observeRecentForProject(projectId: Long, limit: Int = 200): Flow<List<MeasurementLogEntity>> = dao.observeRecentForProject(projectId, limit)
    suspend fun pruneOlderThan(days: Int = 7) {
        val threshold = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        withContext(Dispatchers.IO) { dao.pruneOlderThan(threshold) }
    }
}
