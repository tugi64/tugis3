package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.ProjectEntity
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.prefs.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GraphicalSurveyViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val pointRepo: PointRepository,
    private val prefs: PrefsRepository
) : ViewModel() {

    val activeProject: StateFlow<ProjectEntity?> = projectRepository.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val instrumentHeight: StateFlow<Double> = prefs.instrumentHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // Uygulama ekranı görünürlükleri
    val showToolbar: StateFlow<Boolean> = prefs.appShowToolbar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val showInfoBar: StateFlow<Boolean> = prefs.appShowInfoBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Katman görünürlükleri
    val layerPointsVisible: StateFlow<Boolean> = prefs.layerPointsVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val layerLabelsVisible: StateFlow<Boolean> = prefs.layerLabelsVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val layerTrackVisible: StateFlow<Boolean> = prefs.layerTrackVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Aktif projeye göre noktalar
    val points: Flow<List<PointEntity>> = activeProject
        .flatMapLatest { ap -> if (ap == null) kotlinx.coroutines.flow.flowOf(emptyList()) else pointRepo.observePoints(ap.id) }

    fun setInstrumentHeight(v: Double) { viewModelScope.launch { prefs.setInstrumentHeight(v) } }
    fun setShowToolbar(v: Boolean) { viewModelScope.launch { prefs.setAppShowToolbar(v) } }
    fun setShowInfoBar(v: Boolean) { viewModelScope.launch { prefs.setAppShowInfoBar(v) } }
    fun setLayerPointsVisible(v: Boolean) { viewModelScope.launch { prefs.setLayerPointsVisible(v) } }
    fun setLayerLabelsVisible(v: Boolean) { viewModelScope.launch { prefs.setLayerLabelsVisible(v) } }
    fun setLayerTrackVisible(v: Boolean) { viewModelScope.launch { prefs.setLayerTrackVisible(v) } }
}
