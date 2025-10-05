package com.example.tugis3.data.repository/*
DUPLICATE NOTE:
Bu dosya core/data module içindeki ProjectRepository ile aynı pakette ikinci bir tanım içeriyordu.
Çakışmayı ve derleme hatalarını önlemek için tamamı yorum satırına alındı.
Uygulama artık core module içindeki ProjectRepository'yi (updateUtm vb. fonksiyonları olan) kullanacak.
package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.ProjectDao
import com.example.tugis3.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeProjects()
    fun observeActiveProject(): Flow<ProjectEntity?> = projectDao.observeActiveProject()

    suspend fun createProject(name: String, description: String? = null, activate: Boolean = false): Long {
        if (activate) projectDao.clearActive()
        val entity = ProjectEntity(name = name, description = description, isActive = activate)
        val id = projectDao.upsert(entity)
        if (activate) projectDao.setActive(id)
        return id
    }

    suspend fun setActive(id: Long) {
        projectDao.clearActive(); projectDao.setActive(id)
    }

    suspend fun delete(project: ProjectEntity) = projectDao.delete(project)

    suspend fun updateEllipsoid(projectId: Long, name: String, a: Double, invF: Double) {
        val current = projectDao.getById(projectId) ?: return
        val updated = current.copy(ellipsoidName = name, semiMajorA = a, invFlattening = invF)
        projectDao.upsert(updated)
    }
}
*/
