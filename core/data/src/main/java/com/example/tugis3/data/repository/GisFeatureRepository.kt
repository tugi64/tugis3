package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.GisFeatureDao
import com.example.tugis3.data.db.entity.GisFeatureEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GisFeatureRepository @Inject constructor(
    private val dao: GisFeatureDao
) {
    fun observe(projectId: Long): Flow<List<GisFeatureEntity>> = dao.features(projectId)
    suspend fun add(projectId: Long, type: String, attr: String?) =
        dao.insert(GisFeatureEntity(projectId = projectId, type = type, attr = attr?.ifBlank { null }))
    suspend fun clear(projectId: Long) = dao.clearForProject(projectId)
}

