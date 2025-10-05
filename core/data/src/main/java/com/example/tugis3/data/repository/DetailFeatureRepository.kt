package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.DetailFeatureDao
import com.example.tugis3.data.db.entity.DetailFeatureEntity
import com.example.tugis3.data.db.entity.DetailFeaturePointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailFeatureRepository @Inject constructor(
    private val dao: DetailFeatureDao
) {
    fun observeFeatures(projectId: Long): Flow<List<DetailFeatureEntity>> = dao.observeFeatures(projectId)
    fun observeFeaturesWithPoints(projectId: Long) = dao.observeFeaturesWithPoints(projectId)

    suspend fun insertFeatureWithPoints(projectId: Long, type: String, code: String?, pts: List<Triple<Double, Double, Double?>>) : Long {
        val feature = DetailFeatureEntity(projectId = projectId, type = type, code = code)
        val pointEntities = pts.mapIndexed { idx, p ->
            DetailFeaturePointEntity(
                featureId = 0, // dao method overwrite
                x = p.first,
                y = p.second,
                z = p.third,
                code = code,
                orderIndex = idx,
                timestamp = System.currentTimeMillis()
            )
        }
        return dao.insertFeatureWithPoints(feature, pointEntities)
    }
}
