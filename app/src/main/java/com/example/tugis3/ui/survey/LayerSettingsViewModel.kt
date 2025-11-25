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

/**
 * KATMAN AYARLARI VIEW MODEL
 *
 * SurPad 4.2 Layer Settings İşlevleri:
 * ✅ Katman ekleme (renk, görünürlük, kilit, aktif seçenekleriyle)
 * ✅ Katman silme
 * ✅ Katman yeniden adlandırma
 * ✅ Aktif katman belirleme
 * ✅ Katman görünürlüğü toggle
 * ✅ Katman kilitleme toggle
 * ✅ Tüm katmanları gizle/göster
 * ✅ Katman rengi değiştirme
 */
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

    /**
     * Yeni katman ekle (tüm özelliklerle)
     */
    fun addLayer(
        nameRaw: String,
        colorIndex: Int,
        isWorkLayer: Boolean,
        isVisible: Boolean,
        isLocked: Boolean
    ) {
        val proj = activeProject.value ?: return
        val name = nameRaw.trim().ifBlank { return }

        viewModelScope.launch {
            _busy.value = true
            try {
                // Eğer bu katman aktif katman olacaksa, diğerlerini pasif yap
                if (isWorkLayer) {
                    layerDao.clearAllWorkLayers(proj.id)
                }

                layerDao.insert(
                    CadLayerEntity(
                        projectId = proj.id,
                        name = name,
                        colorIndex = colorIndex,
                        visible = if (isVisible) 1 else 0,
                        locked = if (isLocked) 1 else 0,
                        isWorkLayer = if (isWorkLayer) 1 else 0
                    )
                )
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Katman görünürlüğünü değiştir
     */
    fun toggleVisibility(id: Long, currentVisible: Int) {
        viewModelScope.launch {
            val newVisible = if (currentVisible == 1) 0 else 1
            layerDao.setVisible(id, newVisible, System.currentTimeMillis())
        }
    }

    /**
     * Katman kilitini değiştir
     */
    fun toggleLock(id: Long, currentLocked: Int) {
        viewModelScope.launch {
            val newLocked = if (currentLocked == 1) 0 else 1
            layerDao.setLocked(id, newLocked, System.currentTimeMillis())
        }
    }

    /**
     * Aktif katman (work layer) olarak ayarla
     */
    fun setWorkLayer(id: Long) {
        val proj = activeProject.value ?: return

        viewModelScope.launch {
            _busy.value = true
            try {
                // Önce tüm katmanları pasif yap
                layerDao.clearAllWorkLayers(proj.id)

                // Sonra seçilen katmanı aktif yap
                layerDao.setWorkLayer(id, 1, System.currentTimeMillis())
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Katmanı yeniden adlandır
     */
    fun renameLayer(id: Long, newName: String) {
        val name = newName.trim().ifBlank { return }

        viewModelScope.launch {
            layerDao.rename(id, name, System.currentTimeMillis())
        }
    }

    /**
     * Katmanı sil
     */
    fun deleteLayer(id: Long) {
        viewModelScope.launch {
            layerDao.deleteById(id)
        }
    }

    /**
     * Katman rengini değiştir
     */
    fun changeLayerColor(id: Long, newColorIndex: Int) {
        viewModelScope.launch {
            layerDao.setColor(id, newColorIndex, System.currentTimeMillis())
        }
    }

    /**
     * Tüm katmanları gizle/göster
     */
    fun toggleAllLayersVisibility() {
        val proj = activeProject.value ?: return
        val currentLayers = layers.value

        viewModelScope.launch {
            _busy.value = true
            try {
                // Eğer herhangi bir katman görünürse, hepsini gizle
                // Değilse, hepsini göster
                val anyVisible = currentLayers.any { it.visible == 1 }
                val newVisibility = if (anyVisible) 0 else 1

                layerDao.setAllVisibility(proj.id, newVisibility, System.currentTimeMillis())
            } finally {
                _busy.value = false
            }
        }
    }
}

