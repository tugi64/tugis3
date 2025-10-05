package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.NtripSessionDao
import com.example.tugis3.data.db.entity.NtripSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtripSessionRepository @Inject constructor(
    private val dao: NtripSessionDao
) {
    suspend fun logSession(entity: NtripSessionEntity): Long = dao.insert(entity)
    fun recent(profileId: Long?, limit: Int = 10): Flow<List<NtripSessionEntity>> = dao.recentForProfile(profileId, limit)
}

