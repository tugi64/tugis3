package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.GisFeatureRepository
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GisDataCollectionViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val repo: GisFeatureRepository
) : ViewModel() {
    private val active = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val features = active.flatMapLatest { p -> p?.let { repo.observe(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun add(type: String, attr: String) {
        val proj = active.value ?: return
        val t = type.ifBlank { "Nokta" }
        val a = attr.trim()
        viewModelScope.launch {
            _busy.value = true
            try { repo.add(proj.id, t, a.ifBlank { null }) } finally { _busy.value = false }
        }
    }
}

