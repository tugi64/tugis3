package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.NtripProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NtripProfileDao {
    @Query("SELECT * FROM ntrip_profiles ORDER BY lastUsed DESC")
    fun observeProfiles(): Flow<List<NtripProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: NtripProfileEntity): Long

    @Delete
    suspend fun delete(profile: NtripProfileEntity)

    @Query("UPDATE ntrip_profiles SET autoConnect = 0")
    suspend fun clearAutoConnect()

    @Query("UPDATE ntrip_profiles SET autoConnect = 1 WHERE id = :id")
    suspend fun setAutoConnect(id: Long)
}

