package com.example.tugis3.repository

import com.example.tugis3.data.model.Project
import com.example.tugis3.database.dao.ProjectDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
    
    suspend fun getActiveProject(): Project? = projectDao.getActiveProject()
    
    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)
    
    suspend fun insertProject(project: Project) = projectDao.insertProject(project)
    
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    
    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)
    
    suspend fun deleteProjectById(id: String) = projectDao.deleteProjectById(id)
    
    suspend fun activateProject(id: String) {
        projectDao.deactivateAllProjects()
        projectDao.activateProject(id)
    }
}
