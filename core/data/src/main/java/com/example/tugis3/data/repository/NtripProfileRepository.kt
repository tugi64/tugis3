package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.NtripProfileDao
import com.example.tugis3.data.db.entity.NtripProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtripProfileRepository @Inject constructor(
    private val dao: NtripProfileDao
) {
    fun observeProfiles(): Flow<List<NtripProfileEntity>> = dao.observeProfiles()

    suspend fun upsert(profile: NtripProfileEntity): Long = dao.upsert(profile.copy(lastUsed = System.currentTimeMillis()))
    suspend fun delete(profile: NtripProfileEntity) = dao.delete(profile)
    suspend fun setAutoConnect(id: Long) { dao.clearAutoConnect(); dao.setAutoConnect(id) }
}

