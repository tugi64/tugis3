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

    @Query("UPDATE cad_layers SET locked = :locked, updatedAt = :ts WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Int, ts: Long)

    @Query("UPDATE cad_layers SET isWorkLayer = :isWorkLayer, updatedAt = :ts WHERE id = :id")
    suspend fun setWorkLayer(id: Long, isWorkLayer: Int, ts: Long)

    @Query("UPDATE cad_layers SET isWorkLayer = 0, updatedAt = :ts WHERE projectId = :projectId")
    suspend fun clearAllWorkLayers(projectId: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE cad_layers SET name = :newName, updatedAt = :ts WHERE id = :id")
    suspend fun rename(id: Long, newName: String, ts: Long)

    @Query("UPDATE cad_layers SET colorIndex = :colorIndex, updatedAt = :ts WHERE id = :id")
    suspend fun setColor(id: Long, colorIndex: Int, ts: Long)

    @Query("UPDATE cad_layers SET visible = :visible, updatedAt = :ts WHERE projectId = :projectId")
    suspend fun setAllVisibility(projectId: Long, visible: Int, ts: Long)

    @Query("DELETE FROM cad_layers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cad_layers WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)
}

