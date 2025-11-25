package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.LineDefinitionDao
import com.example.tugis3.data.db.entity.LineDefinitionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineDefinitionRepository @Inject constructor(
    private val lineDefinitionDao: LineDefinitionDao
) {
    fun observeLines(projectId: Long): Flow<List<LineDefinitionEntity>> {
        return lineDefinitionDao.observeLines(projectId)
    }

    suspend fun getLines(projectId: Long): List<LineDefinitionEntity> {
        return lineDefinitionDao.getLines(projectId)
    }

    suspend fun getLineById(id: Long): LineDefinitionEntity? {
        return lineDefinitionDao.getLineById(id)
    }

    fun observeLineById(id: Long): Flow<LineDefinitionEntity?> {
        return lineDefinitionDao.observeLineById(id)
    }

    suspend fun insert(line: LineDefinitionEntity): Long {
        return lineDefinitionDao.insert(line)
    }

    suspend fun update(line: LineDefinitionEntity) {
        lineDefinitionDao.update(line.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(line: LineDefinitionEntity) {
        lineDefinitionDao.delete(line)
    }

    suspend fun deleteById(id: Long) {
        lineDefinitionDao.deleteById(id)
    }

    suspend fun deleteAllByProject(projectId: Long) {
        lineDefinitionDao.deleteAllByProject(projectId)
    }
}

