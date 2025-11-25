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
import javax.inject.Inject

@AndroidEntryPoint
class SurveyManagementActivity : ComponentActivity() {
    private val vm: SurveyManagementViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                SurveyManagementScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}

data class SurveyFile(
    val name: String,
    val size: String,
    val date: String,
    val type: String,
    val selected: Boolean = false
)

@HiltViewModel
class SurveyManagementViewModel @Inject constructor(
    private val projectRepo: ProjectRepository
) : ViewModel() {

    private val _files = MutableStateFlow<List<SurveyFile>>(emptyList())
    val files: StateFlow<List<SurveyFile>> = _files.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _message

    init {
        loadFiles()
    }

    private fun loadFiles() {
        _files.value = listOf(
            SurveyFile("Proje_2025_10_16.sur", "2.3 MB", "16.10.2025", "Survey Data"),
            SurveyFile("Nokta_Listesi_001.csv", "45 KB", "15.10.2025", "Point List"),
            SurveyFile("Koordinatlar.txt", "120 KB", "14.10.2025", "Text File"),
            SurveyFile("Harita_Verileri.dxf", "5.7 MB", "13.10.2025", "CAD Drawing")
        )
    }

    fun toggleSelection(fileName: String) {
        _files.value = _files.value.map {
            if (it.name == fileName) it.copy(selected = !it.selected) else it
        }
    }

    fun createNewSurvey(name: String) {
        viewModelScope.launch {
            _message.tryEmit("✓ Yeni ölçüm dosyası oluşturuldu: $name")
            loadFiles()
        }
    }

    fun deleteSelected() {
        val count = _files.value.count { it.selected }
        _files.value = _files.value.filterNot { it.selected }
        _message.tryEmit("✓ $count dosya silindi")
    }

    fun exportSelected() {
        val count = _files.value.count { it.selected }
        _message.tryEmit("✓ $count dosya dışa aktarıldı")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyManagementScreen(vm: SurveyManagementViewModel, onBack: () -> Unit) {
    val files by vm.files.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showNewFileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ölçüm Yönetimi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewFileDialog = true }) {
                        Icon(Icons.Default.Add, "Yeni Dosya")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // İstatistik Kartı
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("${files.size}", "Dosya")
                    StatColumn("8.2 MB", "Toplam")
                    StatColumn("${files.count { it.selected }}", "Seçili")
                }
            }

            // İşlem Butonları
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Yeni")
                }
                OutlinedButton(
                    onClick = { vm.exportSelected() },
                    modifier = Modifier.weight(1f),
                    enabled = files.any { it.selected }
                ) {
                    Icon(Icons.Default.Upload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dışa Aktar")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.deleteSelected() },
                    modifier = Modifier.weight(1f),
                    enabled = files.any { it.selected },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sil")
                }
            }

            // Dosya Listesi
            Text("Ölçüm Dosyaları", style = MaterialTheme.typography.titleMedium)

            if (files.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Henüz ölçüm dosyası yok")
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files) { file ->
                        SurveyFileCard(file = file, onToggle = { vm.toggleSelection(file.name) })
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Yeni Ölçüm Dosyası") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Dosya Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createNewSurvey(fileName)
                        showNewFileDialog = false
                    },
                    enabled = fileName.isNotBlank()
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SurveyFileCard(file: SurveyFile, onToggle: () -> Unit) {
    ElevatedCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = file.selected, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium)
                Text(
                    "${file.type} • ${file.size} • ${file.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
