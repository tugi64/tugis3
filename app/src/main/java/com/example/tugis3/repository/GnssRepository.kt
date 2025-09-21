package com.example.tugis3.repository

import com.example.tugis3.data.model.GnssData
import com.example.tugis3.database.dao.GnssDataDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnssRepository @Inject constructor(
    private val gnssDataDao: GnssDataDao
) {
    fun getAllData(): Flow<List<GnssData>> = gnssDataDao.getAllData()
    
    fun getDataByProject(projectId: String): Flow<List<GnssData>> = 
        gnssDataDao.getDataByProject(projectId)
    
    suspend fun getDataById(id: String): GnssData? = gnssDataDao.getDataById(id)
    
    suspend fun getDataByPointName(pointName: String, projectId: String): List<GnssData> = 
        gnssDataDao.getDataByPointName(pointName, projectId)
    
    fun getCorsCorrectedData(): Flow<List<GnssData>> = gnssDataDao.getCorsCorrectedData()
    
    suspend fun insertData(data: GnssData) = gnssDataDao.insertData(data)
    
    suspend fun insertDataList(dataList: List<GnssData>) = gnssDataDao.insertDataList(dataList)
    
    suspend fun updateData(data: GnssData) = gnssDataDao.updateData(data)
    
    suspend fun deleteData(data: GnssData) = gnssDataDao.deleteData(data)
    
    suspend fun deleteDataById(id: String) = gnssDataDao.deleteDataById(id)
    
    suspend fun deleteDataByProject(projectId: String) = gnssDataDao.deleteDataByProject(projectId)
    
    suspend fun getDataCountByProject(projectId: String): Int = 
        gnssDataDao.getDataCountByProject(projectId)
}
