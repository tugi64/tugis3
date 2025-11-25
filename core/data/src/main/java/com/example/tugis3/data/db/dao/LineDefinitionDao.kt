package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.LineDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineDefinitionDao {
    @Query("SELECT * FROM line_definitions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeLines(projectId: Long): Flow<List<LineDefinitionEntity>>

    @Query("SELECT * FROM line_definitions WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getLines(projectId: Long): List<LineDefinitionEntity>

    @Query("SELECT * FROM line_definitions WHERE id = :id")
    suspend fun getLineById(id: Long): LineDefinitionEntity?

    @Query("SELECT * FROM line_definitions WHERE id = :id")
    fun observeLineById(id: Long): Flow<LineDefinitionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(line: LineDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<LineDefinitionEntity>)

    @Update
    suspend fun update(line: LineDefinitionEntity)

    @Delete
    suspend fun delete(line: LineDefinitionEntity)

    @Query("DELETE FROM line_definitions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM line_definitions WHERE projectId = :projectId")
    suspend fun deleteAllByProject(projectId: Long)
}

