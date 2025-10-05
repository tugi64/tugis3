package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.CadLayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CadLayerDao {
    @Query("SELECT * FROM cad_layers WHERE projectId = :projectId ORDER BY name")
    fun layers(projectId: Long): Flow<List<CadLayerEntity>>

    @Query("SELECT * FROM cad_layers WHERE projectId = :projectId AND name = :name LIMIT 1")
    suspend fun findByName(projectId: Long, name: String): CadLayerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CadLayerEntity): Long

    @Update
    suspend fun update(entity: CadLayerEntity)

    @Query("UPDATE cad_layers SET visible = :visible, updatedAt = :ts WHERE id = :id")
    suspend fun setVisible(id: Long, visible: Int, ts: Long)
}

