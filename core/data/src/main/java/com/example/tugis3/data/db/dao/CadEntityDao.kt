package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.CadEntityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CadEntityDao {
    @Query("SELECT * FROM cad_entities WHERE projectId = :projectId")
    fun entities(projectId: Long): Flow<List<CadEntityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CadEntityEntity): Long

    @Update
    suspend fun update(entity: CadEntityEntity)

    @Query("DELETE FROM cad_entities WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM cad_entities WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)
}

