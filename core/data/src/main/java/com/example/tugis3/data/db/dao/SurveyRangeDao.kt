package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.SurveyRangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyRangeDao {
    @Query("SELECT * FROM survey_ranges WHERE projectId = :projectId ORDER BY id DESC")
    fun observeRanges(projectId: Long): Flow<List<SurveyRangeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SurveyRangeEntity): Long

    @Update
    suspend fun update(entity: SurveyRangeEntity)

    @Delete
    suspend fun delete(entity: SurveyRangeEntity)

    @Query("DELETE FROM survey_ranges WHERE projectId = :projectId")
    suspend fun clearProject(projectId: Long)
}

