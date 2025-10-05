package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.db.entity.SurveyRangeEntity
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyRangeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SurveyRangeViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val rangeRepo: SurveyRangeRepository
) : ViewModel() {

    private val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ranges: StateFlow<List<SurveyRangeEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else rangeRepo.observe(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress

    fun addRandom() {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _inProgress.value = true
            try {
                val pts = Random.nextInt(3, 40)
                val area = Random.nextDouble(50.0, 10_000.0)
                val perimeter = Random.nextDouble(40.0, 2_000.0)
                val nameBase = "Alan_"
                val existingNames = ranges.value.map { it.name }.toSet()
                var counter = existingNames.size + 1
                var candidate: String
                do {
                    candidate = nameBase + counter.toString().padStart(3, '0')
                    counter++
                } while (candidate in existingNames)
                rangeRepo.add(project.id, candidate, pts, area, perimeter)
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun delete(id: Long) {
        val entity = ranges.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { rangeRepo.delete(entity) }
    }
}

