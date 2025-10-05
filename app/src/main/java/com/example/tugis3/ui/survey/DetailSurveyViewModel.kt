package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.dao.DetailFeatureWithPoints
import com.example.tugis3.data.repository.DetailFeatureRepository
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// DetailPoint data class tanımı
data class DetailPoint(
    val x: Double,
    val y: Double,
    val z: Double?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailSurveyViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val featureRepo: DetailFeatureRepository
) : ViewModel() {

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val featuresWithPoints: StateFlow<List<DetailFeatureWithPoints>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else featureRepo.observeFeaturesWithPoints(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveFeature(type: String, code: String?, points: List<DetailPoint>) {
        val proj = activeProject.value ?: return
        if (points.isEmpty()) return
        viewModelScope.launch {
            featureRepo.insertFeatureWithPoints(
                projectId = proj.id,
                type = type,
                code = code?.ifBlank { null },
                pts = points.map { Triple(it.x, it.y, it.z) }
            )
        }
    }

    fun exportFeaturesCsv(): String {
        val list = featuresWithPoints.value
        if (list.isEmpty()) return ""
        val header = listOf("featureId","projectId","type","code","pointIndex","x","y","z","timestampFeature","timestampPoint").joinToString(",")
        val rows = buildList {
            list.forEach { f ->
                if (f.points.isEmpty()) {
                    add(listOf(f.feature.id, f.feature.projectId, f.feature.type, f.feature.code ?: "", "", "", "", "", f.feature.createdAt, "").joinToString(","))
                } else {
                    f.points.forEachIndexed { idx, p ->
                        add(listOf(f.feature.id, f.feature.projectId, f.feature.type, f.feature.code ?: "", idx, p.x, p.y, p.z ?: "", f.feature.createdAt, p.timestamp).joinToString(","))
                    }
                }
            }
        }.joinToString("\n")
        return header + "\n" + rows
    }
}
