package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.CalibrationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationPointDao {
    @Query("SELECT * FROM calibration_points WHERE projectId = :projectId ORDER BY id ASC")
    fun observe(projectId: Long): Flow<List<CalibrationPointEntity>>

    @Query("SELECT * FROM calibration_points WHERE projectId = :projectId AND include = 1")
    suspend fun getIncluded(projectId: Long): List<CalibrationPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CalibrationPointEntity): Long

    @Update
    suspend fun update(entity: CalibrationPointEntity)

    @Delete
    suspend fun delete(entity: CalibrationPointEntity)

    @Query("UPDATE calibration_points SET include = :include, updatedAt = :ts WHERE id = :id")
    suspend fun setInclude(id: Long, include: Int, ts: Long)

    @Query("DELETE FROM calibration_points WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)
}

