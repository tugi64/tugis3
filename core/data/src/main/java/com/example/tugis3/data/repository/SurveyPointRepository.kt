package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.SurveyPointDao
import com.example.tugis3.data.db.entity.SurveyPointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurveyPointRepository @Inject constructor(
    private val dao: SurveyPointDao
) {
    fun observePoints(projectId: Long): Flow<List<SurveyPointEntity>> = dao.observePoints(projectId)

    fun observeAllPoints(): Flow<List<SurveyPointEntity>> = dao.observeAllPoints()

    suspend fun insert(entity: SurveyPointEntity): Long = dao.insert(entity.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(entity: SurveyPointEntity) = dao.delete(entity)

    suspend fun clearForProject(projectId: Long) = dao.clearForProject(projectId)

    suspend fun saveBasePoint(
        name: String,
        code: String?,
        latitude: Double?,
        longitude: Double?,
        elevation: Double?,
        northing: Double?,
        easting: Double?,
        zone: String?,
        hrms: Double?,
        vrms: Double?,
        pdop: Double?,
        satellites: Int?,
        fixType: String?,
        antennaHeight: Double?,
        projectId: Long
    ): Long {
        val entity = SurveyPointEntity(
            projectId = projectId,
            name = name,
            code = code,
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            northing = northing,
            easting = easting,
            zone = zone,
            hrms = hrms,
            vrms = vrms,
            pdop = pdop,
            satellites = satellites,
            fixType = fixType,
            antennaHeight = antennaHeight,
            timestamp = System.currentTimeMillis()
        )
        return insert(entity)
    }
}
