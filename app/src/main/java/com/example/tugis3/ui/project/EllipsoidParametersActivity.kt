package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.Ellipsoids
import com.example.tugis3.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EllipsoidParametersActivity : ComponentActivity() {
    private val vm: EllipsoidParametersViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EllipsoidParametersScreen(vm = vm, onBack = { finish() }) }
    }
}

@HiltViewModel
class EllipsoidParametersViewModel @Inject constructor(
    private val projectRepo: ProjectRepository
) : ViewModel() {
    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedEllipsoidName = activeProject.map { it?.ellipsoidName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun applyEllipsoid(name: String, a: Double, invF: Double) = viewModelScope.launch {
        val proj = activeProject.value ?: return@launch
        projectRepo.updateEllipsoid(proj.id, name, a, invF)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EllipsoidParametersScreen(vm: EllipsoidParametersViewModel, onBack: () -> Unit) {
    val list = remember { Ellipsoids.all }
    val selected by vm.selectedEllipsoidName.collectAsState()
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elipsoid Parametreleri") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list) { e ->
                val isSelected = e.name == selected
                ElevatedCard(
                    onClick = {
                        vm.applyEllipsoid(e.name, e.semiMajorA, e.invFlattening)
                        scope.launch { snackHost.showSnackbar("Seçildi: ${e.name}") }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.Bold)
                            Text(
                                "a=%.3f 1/f=%.9f".format(e.semiMajorA, e.invFlattening),
                                style = MaterialTheme.typography.bodySmall
                            )
                            e.description?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
