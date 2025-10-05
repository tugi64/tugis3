package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    fun observeActiveProject(): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity): Long

    @Query("UPDATE projects SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE projects SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Delete
    suspend fun delete(project: ProjectEntity)
}
