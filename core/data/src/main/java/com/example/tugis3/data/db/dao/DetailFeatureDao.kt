package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.DetailFeatureEntity
import com.example.tugis3.data.db.entity.DetailFeaturePointEntity
import kotlinx.coroutines.flow.Flow
import androidx.room.Embedded
import androidx.room.Relation

@Dao
interface DetailFeatureDao {
    @Query("SELECT * FROM detail_features WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeFeatures(projectId: Long): Flow<List<DetailFeatureEntity>>

    @Transaction
    @Query("SELECT * FROM detail_features WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeFeaturesWithPoints(projectId: Long): Flow<List<DetailFeatureWithPoints>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeature(feature: DetailFeatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<DetailFeaturePointEntity>)

    @Transaction
    suspend fun insertFeatureWithPoints(feature: DetailFeatureEntity, pts: List<DetailFeaturePointEntity>) : Long {
        val id = insertFeature(feature)
        if (pts.isNotEmpty()) {
            insertPoints(pts.mapIndexed { idx, p -> p.copy(featureId = id, orderIndex = idx) })
        }
        return id
    }

    @Query("SELECT * FROM detail_feature_points WHERE featureId = :featureId ORDER BY orderIndex ASC")
    suspend fun getPoints(featureId: Long): List<DetailFeaturePointEntity>

    @Delete
    suspend fun deleteFeature(feature: DetailFeatureEntity)

    @Query("DELETE FROM detail_feature_points WHERE featureId = :featureId")
    suspend fun deletePointsOfFeature(featureId: Long)
}

data class DetailFeatureWithPoints(
    @Embedded val feature: DetailFeatureEntity,
    @Relation(parentColumn = "id", entityColumn = "featureId")
    val points: List<DetailFeaturePointEntity>
)
