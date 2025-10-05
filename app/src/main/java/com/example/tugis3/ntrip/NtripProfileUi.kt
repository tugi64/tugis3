package com.example.tugis3.ntrip

import com.example.tugis3.data.db.entity.NtripProfileEntity

/** UI katmanı için sade model */
data class NtripProfileUi(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val mountPoint: String,
    val username: String? = null,
    val password: String? = null,
    val autoConnect: Boolean = false
)

// Entity dönüşümü
fun NtripProfileUi.toEntity() = NtripProfileEntity(
    id = id,
    name = name,
    host = host,
    port = port,
    mountPoint = mountPoint,
    username = username,
    password = password,
    autoConnect = autoConnect,
    lastUsed = System.currentTimeMillis()
)
