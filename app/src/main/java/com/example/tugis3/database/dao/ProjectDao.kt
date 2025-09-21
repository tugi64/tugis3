package com.example.tugis3.database.dao

import androidx.room.*
import com.example.tugis3.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects WHERE isActive = 1")
    suspend fun getActiveProject(): Project?
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)
    
    @Update
    suspend fun updateProject(project: Project)
    
    @Delete
    suspend fun deleteProject(project: Project)
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
    
    @Query("UPDATE projects SET isActive = 0")
    suspend fun deactivateAllProjects()
    
    @Query("UPDATE projects SET isActive = 1 WHERE id = :id")
    suspend fun activateProject(id: String)
}
