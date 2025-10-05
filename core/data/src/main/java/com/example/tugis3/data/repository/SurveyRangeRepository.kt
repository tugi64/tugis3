package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.SurveyRangeDao
import com.example.tugis3.data.db.entity.SurveyRangeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurveyRangeRepository @Inject constructor(
    private val dao: SurveyRangeDao
) {
    fun observe(projectId: Long): Flow<List<SurveyRangeEntity>> = dao.observeRanges(projectId)

    suspend fun add(projectId: Long, name: String, pointCount: Int, area: Double?, perimeter: Double?): Long =
        dao.insert(
            SurveyRangeEntity(
                projectId = projectId,
                name = name,
                pointCount = pointCount,
                area = area,
                perimeter = perimeter
            )
        )

    suspend fun update(entity: SurveyRangeEntity) = dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(entity: SurveyRangeEntity) = dao.delete(entity)
    suspend fun clear(projectId: Long) = dao.clearProject(projectId)
}

