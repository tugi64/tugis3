package com.example.tugis3.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.ProjectEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectManagerViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepo.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProject: StateFlow<ProjectEntity?> = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, desc: String?, activate: Boolean) = viewModelScope.launch {
        projectRepo.createProject(name = name, description = desc, activate = activate)
    }

    fun setActive(id: Long) = viewModelScope.launch { projectRepo.setActive(id) }

    fun delete(id: Long) = viewModelScope.launch {
        val target = projects.value.firstOrNull { it.id == id } ?: return@launch
        // Noktaları da sil
        pointRepo.deleteByProject(id)
        projectRepo.delete(target)
    }
}

