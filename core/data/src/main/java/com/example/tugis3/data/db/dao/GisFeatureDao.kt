package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.GisFeatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GisFeatureDao {
    @Query("SELECT * FROM gis_features WHERE projectId = :projectId ORDER BY id DESC")
    fun features(projectId: Long): Flow<List<GisFeatureEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: GisFeatureEntity): Long

    @Delete
    suspend fun delete(entity: GisFeatureEntity)

    @Query("DELETE FROM gis_features WHERE projectId = :projectId")
    suspend fun clearForProject(projectId: Long)
}

