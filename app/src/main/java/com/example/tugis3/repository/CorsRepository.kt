package com.example.tugis3.repository

import com.example.tugis3.data.model.CorsStation
import com.example.tugis3.database.dao.CorsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorsRepository @Inject constructor(
    private val corsDao: CorsDao
) {
    fun getAllStations(): Flow<List<CorsStation>> = corsDao.getAllStations()
    
    fun getActiveStations(): Flow<List<CorsStation>> = corsDao.getActiveStations()
    
    suspend fun getStationById(id: String): CorsStation? = corsDao.getStationById(id)
    
    suspend fun insertStation(station: CorsStation) = corsDao.insertStation(station)
    
    suspend fun insertStations(stations: List<CorsStation>) = corsDao.insertStations(stations)
    
    suspend fun updateStation(station: CorsStation) = corsDao.updateStation(station)
    
    suspend fun deleteStation(station: CorsStation) = corsDao.deleteStation(station)
    
    suspend fun deleteStationById(id: String) = corsDao.deleteStationById(id)
    
    suspend fun updateStationStatus(id: String, isActive: Boolean) = 
        corsDao.updateStationStatus(id, isActive)
}
