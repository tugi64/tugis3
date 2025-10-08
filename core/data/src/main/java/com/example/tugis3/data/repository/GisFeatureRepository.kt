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

    // Yeni: geometry ve layer ile ekleme (GeoJSON geometryJson)
    suspend fun add(
        projectId: Long,
        type: String,
        attr: String?,
        geometryJson: String?,
        layer: String? = null
    ): Long = dao.insert(
        GisFeatureEntity(
            projectId = projectId,
            type = type,
            attr = attr?.ifBlank { null },
            layer = layer,
            geometryJson = geometryJson
        )
    )

    suspend fun updateGeometry(id: Long, geometryJson: String) =
        dao.updateGeometry(id, geometryJson, System.currentTimeMillis())

    suspend fun get(id: Long): GisFeatureEntity? = dao.getById(id)

    suspend fun delete(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun clear(projectId: Long) = dao.clearForProject(projectId)
}
