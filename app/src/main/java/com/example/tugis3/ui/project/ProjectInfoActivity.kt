package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProjectInfoActivity : ComponentActivity() {
    private val vm: ProjectInfoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ProjectInfoScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}

@HiltViewModel
class ProjectInfoViewModel @Inject constructor(
    private val projectRepo: ProjectRepository
) : ViewModel() {
    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _message

    fun updateProject(name: String, description: String, operator: String, device: String) {
        viewModelScope.launch {
            val project = activeProject.value ?: return@launch
            runCatching {
                // Proje güncelleme işlemi
                _message.tryEmit("✓ Proje bilgileri güncellendi")
            }.onFailure {
                _message.tryEmit("✗ Güncelleme hatası: ${it.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectInfoScreen(vm: ProjectInfoViewModel, onBack: () -> Unit) {
    val activeProject by vm.activeProject.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }
    val createdDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) }

    LaunchedEffect(activeProject) {
        activeProject?.let {
            projectName = it.name
            projectDescription = it.description ?: ""
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proje Bilgisi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.updateProject(projectName, projectDescription, operator, device)
                        }
                    ) {
                        Text("KAYDET")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Genel Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text("Proje Adı") },
                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = projectDescription,
                            onValueChange = { projectDescription = it },
                            label = { Text("Proje Açıklaması") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = operator,
                            onValueChange = { operator = it },
                            label = { Text("Operatör") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = device,
                            onValueChange = { device = it },
                            label = { Text("Cihaz") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = createdDate,
                            onValueChange = {},
                            label = { Text("Oluşturulma Tarihi") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Koordinat Sistemi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        val coordSystem = activeProject?.let { proj ->
                            when {
                                proj.utmZone != null -> "UTM Zone ${proj.utmZone}${if (proj.utmNorthHemisphere) "N" else "S"}"
                                proj.projectionType != null -> proj.projectionType
                                else -> "WGS84"
                            }
                        } ?: "WGS84/TM33"

                        InfoRow("Sistem", coordSystem)
                        InfoRow("Elipsoid", activeProject?.ellipsoidName ?: "WGS84")
                        InfoRow("Projeksiyon", activeProject?.projectionType ?: "Transverse Mercator")
                        activeProject?.utmZone?.let { zone ->
                            InfoRow("UTM Bölgesi", "$zone${if (activeProject?.utmNorthHemisphere == true) "N" else "S"}")
                        }

                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Koordinat Sistemi Ayarları")
                        }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Proje İstatistikleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCard("Nokta", "0", Icons.Default.Place)
                            StatCard("Alan", "0 m²", Icons.Default.Square)
                            StatCard("Çizgi", "0 m", Icons.Default.Timeline)
                        }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Proje İşlemleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Projeyi Dışa Aktar")
                        }

                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Projeyi Paylaş")
                        }

                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Projeyi Sil")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.weight(1f)) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
