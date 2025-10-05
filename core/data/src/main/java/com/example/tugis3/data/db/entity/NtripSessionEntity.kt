package com.example.tugis3.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "ntrip_sessions",
    indices = [Index(value = ["profileId"])]
)
data class NtripSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long?,
    val startTs: Long,
    val endTs: Long,
    val rtcmBytes: Long,
    val nmeaBytes: Long,
    val avgRateBps: Double,
    val maxRateBps: Double,
    val corrections: Int,
    val finalFix: String?,
    val simulated: Int
)
