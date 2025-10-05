package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PointDao {
    @Query("SELECT * FROM points WHERE projectId = :projectId AND deleted = 0 ORDER BY createdAt DESC")
    fun observePoints(projectId: Long): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE projectId = :projectId AND deleted = 1 ORDER BY deletedAt DESC LIMIT :limit")
    fun observeDeletedPoints(projectId: Long, limit: Int = 20): Flow<List<PointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(point: PointEntity): Long

    @Delete
    suspend fun delete(point: PointEntity)

    @Query("DELETE FROM points WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query("UPDATE points SET name = :name, northing = :northing, easting = :easting, ellipsoidalHeight = :height, featureCode = :code, description = :desc, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePointFull(id: Long, name: String, northing: Double, easting: Double, height: Double?, code: String?, desc: String?, updatedAt: Long)

    // Soft delete / restore
    @Query("UPDATE points SET deleted = 1, deletedAt = :ts, updatedAt = :ts WHERE id IN (:ids)")
    suspend fun softDelete(ids: List<Long>, ts: Long)

    @Query("UPDATE points SET deleted = 0, deletedAt = NULL, updatedAt = :ts WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>, ts: Long)

    // Duplicate name kontrolü
    @Query("SELECT COUNT(*) FROM points WHERE projectId = :projectId AND name = :name AND deleted = 0 AND id != :excludeId")
    suspend fun countName(projectId: Long, name: String, excludeId: Long = 0): Int

    @Query("SELECT * FROM points WHERE projectId = :projectId AND deleted = 0 AND (name LIKE :pattern OR featureCode LIKE :pattern OR description LIKE :pattern) ORDER BY createdAt DESC")
    fun searchPoints(projectId: Long, pattern: String): Flow<List<PointEntity>>
}
