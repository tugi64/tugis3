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
    suspend fun upsert(entity: NtripProfileEntity): Long = dao.upsert(entity.copy(lastUsed = System.currentTimeMillis()))
    suspend fun delete(entity: NtripProfileEntity) = dao.delete(entity)
    suspend fun setAutoConnect(entity: NtripProfileEntity) {
        dao.clearAutoConnect()
        dao.setAutoConnect(entity.id)
    }
}

