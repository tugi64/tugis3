package com.example.tugis3.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tugis3.data.db.entity.NtripSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NtripSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NtripSessionEntity): Long

    @Query("SELECT * FROM ntrip_sessions WHERE profileId = :profileId ORDER BY startTs DESC LIMIT :limit")
    fun recentForProfile(profileId: Long?, limit: Int = 20): Flow<List<NtripSessionEntity>>

    @Query("SELECT COUNT(*) FROM ntrip_sessions")
    suspend fun count(): Long
}

