package com.example.tugis3.database.dao

import androidx.room.*
import com.example.tugis3.data.model.CorsStation
import kotlinx.coroutines.flow.Flow

@Dao
interface CorsDao {
    @Query("SELECT * FROM cors_stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<CorsStation>>

    @Query("SELECT * FROM cors_stations WHERE isActive = 1")
    fun getActiveStations(): Flow<List<CorsStation>>

    @Query("SELECT * FROM cors_stations WHERE id = :id")
    suspend fun getStationById(id: String): CorsStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: CorsStation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<CorsStation>)

    @Update
    suspend fun updateStation(station: CorsStation): Int

    @Delete
    suspend fun deleteStation(station: CorsStation): Int

    @Query("DELETE FROM cors_stations WHERE id = :id")
    suspend fun deleteStationById(id: String): Int

    @Query("UPDATE cors_stations SET isActive = :isActive WHERE id = :id")
    suspend fun updateStationStatus(id: String, isActive: Boolean): Int
}