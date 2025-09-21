package com.example.tugis3.database.dao

import androidx.room.*
import com.example.tugis3.data.model.GnssData
import kotlinx.coroutines.flow.Flow

@Dao
interface GnssDataDao {
    @Query("SELECT * FROM gnss_data ORDER BY timestamp DESC")
    fun getAllData(): Flow<List<GnssData>>
    
    @Query("SELECT * FROM gnss_data WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getDataByProject(projectId: String): Flow<List<GnssData>>
    
    @Query("SELECT * FROM gnss_data WHERE id = :id")
    suspend fun getDataById(id: String): GnssData?
    
    @Query("SELECT * FROM gnss_data WHERE pointName = :pointName AND projectId = :projectId")
    suspend fun getDataByPointName(pointName: String, projectId: String): List<GnssData>
    
    @Query("SELECT * FROM gnss_data WHERE isCorsCorrected = 1 ORDER BY timestamp DESC")
    fun getCorsCorrectedData(): Flow<List<GnssData>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: GnssData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataList(dataList: List<GnssData>)
    
    @Update
    suspend fun updateData(data: GnssData)
    
    @Delete
    suspend fun deleteData(data: GnssData)
    
    @Query("DELETE FROM gnss_data WHERE id = :id")
    suspend fun deleteDataById(id: String)
    
    @Query("DELETE FROM gnss_data WHERE projectId = :projectId")
    suspend fun deleteDataByProject(projectId: String)
    
    @Query("SELECT COUNT(*) FROM gnss_data WHERE projectId = :projectId")
    suspend fun getDataCountByProject(projectId: String): Int
}
