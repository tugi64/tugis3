package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.SurveyPointDao
import com.example.tugis3.data.db.entity.SurveyPointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurveyPointRepository @Inject constructor(
    private val dao: SurveyPointDao
) {
    fun observePoints(projectId: Long): Flow<List<SurveyPointEntity>> = dao.observePoints(projectId)

    suspend fun insert(entity: SurveyPointEntity): Long = dao.insert(entity.copy(updatedAt = System.currentTimeMillis()))

    suspend fun clearForProject(projectId: Long) = dao.clearForProject(projectId)
}
