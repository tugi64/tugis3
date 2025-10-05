package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ntrip_profiles")
data class NtripProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 2101,
    val mountPoint: String,
    val username: String? = null,
    val password: String? = null,
    val autoConnect: Boolean = false,
    val lastUsed: Long = System.currentTimeMillis()
)

