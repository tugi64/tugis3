package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.SurveyPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyPointDao {
    @Query("SELECT * FROM survey_points WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun observePoints(projectId: Long): Flow<List<SurveyPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: SurveyPointEntity): Long

    @Update
    suspend fun update(point: SurveyPointEntity)

    @Delete
    suspend fun delete(point: SurveyPointEntity)

    @Query("DELETE FROM survey_points WHERE projectId = :projectId")
    suspend fun clearForProject(projectId: Long)
}
