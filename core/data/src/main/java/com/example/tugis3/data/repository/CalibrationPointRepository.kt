package com.example.tugis3.data.repository

import com.example.tugis3.data.db.dao.CalibrationPointDao
import com.example.tugis3.data.db.dao.ProjectDao
import com.example.tugis3.data.db.entity.CalibrationPointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationPointRepository @Inject constructor(
    private val dao: CalibrationPointDao,
    private val projectDao: ProjectDao
) {
    fun observe(projectId: Long): Flow<List<CalibrationPointEntity>> = dao.observe(projectId)

    suspend fun add(
        projectId: Long,
        srcNorth: Double,
        srcEast: Double,
        dstNorth: Double,
        dstEast: Double,
        weight: Double = 1.0
    ): Long = dao.insert(
        CalibrationPointEntity(
            projectId = projectId,
            srcNorth = srcNorth,
            srcEast = srcEast,
            dstNorth = dstNorth,
            dstEast = dstEast,
            weight = weight
        )
    )

    suspend fun toggleInclude(id: Long, include: Boolean) = dao.setInclude(id, if (include) 1 else 0, System.currentTimeMillis())
    suspend fun delete(entity: CalibrationPointEntity) = dao.delete(entity)

    data class SolveResult(
        val scale: Double,
        val rotRad: Double,
        val tx: Double,
        val ty: Double,
        val rms: Double,
        val pointCount: Int
    )

    suspend fun solveAndApply(projectId: Long): Result<SolveResult> = runCatching {
        val pts = dao.getIncluded(projectId)
        if (pts.size < 2) error("Yetersiz nokta (en az 2)")
        val solverInput: List<com.example.tugis3.data.repository.LocalizationSolver.Point> = pts.map { p ->
            com.example.tugis3.data.repository.LocalizationSolver.Point(
                srcE = p.srcEast,
                srcN = p.srcNorth,
                dstE = p.dstEast,
                dstN = p.dstNorth,
                w = p.weight
            )
        }
        val params = com.example.tugis3.data.repository.LocalizationSolver.solveSimilarity(solverInput)
        val scale = params.scale
        val rot = params.rot
        val tx = params.tx
        val ty = params.ty
        val cosR = kotlin.math.cos(rot)
        val sinR = kotlin.math.sin(rot)
        var sumSq = 0.0
        solverInput.forEach { pt ->
            val predE = scale * (cosR * pt.srcE - sinR * pt.srcN) + tx
            val predN = scale * (sinR * pt.srcE + cosR * pt.srcN) + ty
            val dE = pt.dstE - predE
            val dN = pt.dstN - predN
            sumSq += dE * dE + dN * dN
        }
        val rms = kotlin.math.sqrt(sumSq / (2.0 * solverInput.size))
        projectDao.updateLocalization(
            projectId = projectId,
            scale = scale,
            rot = rot,
            tx = tx,
            ty = ty,
            pointCount = pts.size,
            ts = System.currentTimeMillis()
        )
        SolveResult(scale, rot, tx, ty, rms, pts.size)
    }
}
