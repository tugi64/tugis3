package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.dao.CadLayerDao
import com.example.tugis3.data.db.entity.CadLayerEntity
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class LayerSettingsViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val layerDao: CadLayerDao
) : ViewModel() {

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val layers: StateFlow<List<CadLayerEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else layerDao.layers(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun addLayer(nameRaw: String) {
        val proj = activeProject.value ?: return
        val name = nameRaw.trim().ifBlank { return }
        viewModelScope.launch {
            _busy.value = true
            try {
                val colorIndex = Random.nextInt(0, 255)
                layerDao.insert(
                    CadLayerEntity(
                        projectId = proj.id,
                        name = name,
                        colorIndex = colorIndex,
                        visible = 1
                    )
                )
            } finally { _busy.value = false }
        }
    }

    fun toggleVisibility(id: Long, currentVisible: Int) {
        viewModelScope.launch {
            layerDao.setVisible(id, if (currentVisible == 1) 0 else 1, System.currentTimeMillis())
        }
    }
}

