package com.example.tugis3.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.ProjectEntity
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Project ana menüsü için durum yöneticisi
 * - Proje listesi (akış)
 * - Aktif proje
 * - Oluştur / Aktif Yap / Sil
 * - UI durumları (loading, error)
 */
@HiltViewModel
class ProjectMenuViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0) // ileride manuel yenileme için
    private val _inProgress = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val ui: StateFlow<ProjectMenuUiState> = combine(
        projectRepository.observeProjects(),
        projectRepository.observeActiveProject(),
        _inProgress,
        _error,
        refreshTrigger
    ) { list, active, loading, error, _ ->
        val mapped = list.map { it.toUi(activeId = active?.id) }
        ProjectMenuUiState(
            projects = mapped,
            activeProject = mapped.firstOrNull { it.isActive },
            loading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectMenuUiState())

    fun createProject(name: String, desc: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _inProgress.value = true
            _error.value = null
            try {
                projectRepository.createProject(name.trim(), desc?.trim().takeUnless { it.isNullOrBlank() }, activate = true)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Bilinmeyen hata"
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun setActive(id: Long) {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                projectRepository.setActive(id)
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                val current = ui.value.projects.find { it.id == id } ?: return@launch
                projectRepository.delete(
                    ProjectEntity(
                        id = current.id,
                        name = current.name,
                        createdAt = current.createdAt,
                        isActive = current.isActive,
                        description = current.description,
                        ellipsoidName = current.ellipsoidName,
                        semiMajorA = current.semiMajorA,
                        invFlattening = current.invFlattening,
                        utmZone = current.utmZone,
                        utmNorthHemisphere = current.utmNorthHemisphere,
                        epsgCode = current.epsgCode,
                        projectionType = current.projectionType,
                        projCentralMeridianDeg = current.projCentralMeridianDeg,
                        projFalseNorthing = current.projFalseNorthing,
                        projFalseEasting = current.projFalseEasting,
                        projScaleFactor = current.projScaleFactor,
                        projLatOrigin = current.projLatOrigin,
                        projStdParallel1 = current.projStdParallel1,
                        projStdParallel2 = current.projStdParallel2,
                        locScale = current.locScale,
                        locRotRad = current.locRotRad,
                        locTx = current.locTx,
                        locTy = current.locTy,
                        locPointCount = current.locPointCount,
                        locLastSolvedAt = current.locLastSolvedAt
                    )
                )
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}

private fun ProjectEntity.toUi(activeId: Long?): ProjectUiModel = ProjectUiModel(
    id = id,
    name = name,
    createdAt = createdAt,
    isActive = id == activeId,
    description = description,
    ellipsoidName = ellipsoidName,
    semiMajorA = semiMajorA,
    invFlattening = invFlattening,
    utmZone = utmZone,
    utmNorthHemisphere = utmNorthHemisphere,
    epsgCode = epsgCode,
    projectionType = projectionType,
    projCentralMeridianDeg = projCentralMeridianDeg,
    projFalseNorthing = projFalseNorthing,
    projFalseEasting = projFalseEasting,
    projScaleFactor = projScaleFactor,
    projLatOrigin = projLatOrigin,
    projStdParallel1 = projStdParallel1,
    projStdParallel2 = projStdParallel2,
    locScale = locScale,
    locRotRad = locRotRad,
    locTx = locTx,
    locTy = locTy,
    locPointCount = locPointCount,
    locLastSolvedAt = locLastSolvedAt
)

// UI katmanı için sade model
data class ProjectUiModel(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isActive: Boolean,
    val description: String?,
    val ellipsoidName: String?,
    val semiMajorA: Double?,
    val invFlattening: Double?,
    val utmZone: Int?,
    val utmNorthHemisphere: Boolean,
    val epsgCode: Int?,
    val projectionType: String?,
    val projCentralMeridianDeg: Double?,
    val projFalseNorthing: Double?,
    val projFalseEasting: Double?,
    val projScaleFactor: Double?,
    val projLatOrigin: Double?,
    val projStdParallel1: Double?,
    val projStdParallel2: Double?,
    val locScale: Double?,
    val locRotRad: Double?,
    val locTx: Double?,
    val locTy: Double?,
    val locPointCount: Int?,
    val locLastSolvedAt: Long?
)

data class ProjectMenuUiState(
    val projects: List<ProjectUiModel> = emptyList(),
    val activeProject: ProjectUiModel? = null,
    val loading: Boolean = false,
    val error: String? = null
)
