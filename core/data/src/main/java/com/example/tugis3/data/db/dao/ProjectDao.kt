package com.example.tugis3.data.db.dao

import androidx.room.*
import com.example.tugis3.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    fun observeActiveProject(): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity): Long

    @Query("UPDATE projects SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE projects SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Delete
    suspend fun delete(project: ProjectEntity)

    // --- Localization parametre güncelleme ---
    @Query("UPDATE projects SET locScale=:scale, locRotRad=:rot, locTx=:tx, locTy=:ty, locPointCount=:pointCount, locLastSolvedAt=:ts WHERE id=:projectId")
    suspend fun updateLocalization(
        projectId: Long,
        scale: Double?,
        rot: Double?,
        tx: Double?,
        ty: Double?,
        pointCount: Int?,
        ts: Long?
    )

    // --- UTM / EPSG parametre güncelleme ---
    @Query("UPDATE projects SET utmZone = :zone, utmNorthHemisphere = :north, epsgCode = :epsg WHERE id = :projectId")
    suspend fun updateUtm(
        projectId: Long,
        zone: Int?,
        north: Boolean,
        epsg: Int?
    )

    // --- Gelişmiş projeksiyon parametre güncelleme ---
    @Query("UPDATE projects SET projectionType=:type, projCentralMeridianDeg=:cm, projFalseNorthing=:fn, projFalseEasting=:fe, projScaleFactor=:k0, projLatOrigin=:lat0, projStdParallel1=:sp1, projStdParallel2=:sp2 WHERE id=:projectId")
    suspend fun updateProjectionAdvanced(
        projectId: Long,
        type: String?,
        cm: Double?,
        fn: Double?,
        fe: Double?,
        k0: Double?,
        lat0: Double?,
        sp1: Double?,
        sp2: Double?
    )
}
