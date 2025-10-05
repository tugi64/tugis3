package com.example.tugis3.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tugis3.data.db.entity.MeasurementLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MeasurementLogEntity): Long

    @Query("SELECT * FROM measurement_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<MeasurementLogEntity>>

    @Query("SELECT * FROM measurement_logs WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentForProject(projectId: Long, limit: Int = 200): Flow<List<MeasurementLogEntity>>

    @Query("DELETE FROM measurement_logs WHERE createdAt < :thresholdTs")
    suspend fun pruneOlderThan(thresholdTs: Long)
}
