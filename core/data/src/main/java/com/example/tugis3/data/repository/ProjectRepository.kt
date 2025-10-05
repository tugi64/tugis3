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

    suspend fun updateLocalizationParams(
        projectId: Long,
        scale: Double?,
        rot: Double?,
        tx: Double?,
        ty: Double?,
        pointCount: Int?,
        ts: Long?
    ) {
        projectDao.updateLocalization(projectId, scale, rot, tx, ty, pointCount, ts)
    }

    // --- UTM / EPSG parametre güncelleme ---
    suspend fun updateUtm(projectId: Long, zone: Int?, north: Boolean, epsg: Int?) {
        projectDao.updateUtm(projectId, zone, north, epsg)
    }

    suspend fun updateProjectionAdvanced(
        projectId: Long,
        type: String?,
        cm: Double?,
        fn: Double?,
        fe: Double?,
        k0: Double?,
        lat0: Double?,
        sp1: Double?,
        sp2: Double?
    ) {
        projectDao.updateProjectionAdvanced(projectId, type, cm, fn, fe, k0, lat0, sp1, sp2)
    }
}
