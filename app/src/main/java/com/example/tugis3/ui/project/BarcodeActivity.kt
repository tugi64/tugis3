package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.db.entity.PointEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class BarcodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                BarcodeScreen(onBackPressed = { finish() })
            }
        }
    }
}

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository
) : ViewModel() {
    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun savePoint(name: String, desc: String?, easting: Double?, northing: Double?, height: Double?) {
        val proj = activeProject.value ?: run {
            _messages.tryEmit("Aktif proje yok")
            return
        }
        if (name.isBlank()) {
            _messages.tryEmit("Nokta adı boş")
            return
        }
        if (easting == null || northing == null) {
            _messages.tryEmit("X/Y koordinat girilmeli")
            return
        }
        viewModelScope.launch {
            runCatching {
                pointRepo.upsert(
                    PointEntity(
                        projectId = proj.id,
                        name = name.trim(),
                        northing = northing,
                        easting = easting,
                        ellipsoidalHeight = height,
                        featureCode = "BARCODE",
                        description = desc?.ifBlank { null }
                    )
                )
            }.onSuccess { _messages.tryEmit("Kaydedildi (#$it)") }
                .onFailure { _messages.tryEmit("Hata: ${it.message}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScreen(onBackPressed: () -> Unit, vm: BarcodeViewModel = hiltViewModel()) {
    var scannedCode by remember { mutableStateOf("") }
    var pointName by remember { mutableStateOf("") }
    var pointDescription by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var east by remember { mutableStateOf("") }
    var north by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barkod Okuyucu") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barkod Okuma Alanı
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Barkod Okuma",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Barkod",
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            isScanning = !isScanning
                            if (isScanning) {
                                // Simüle edilmiş barkod okuma
                                scannedCode = "P${(1000..9999).random()}"
                                pointName = "Nokta_$scannedCode"
                                isScanning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isScanning) "Tarama Yapılıyor..." else "Barkod Tara")
                    }

                    if (scannedCode.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Okunan Kod:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    scannedCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Nokta Bilgileri
            if (scannedCode.isNotEmpty()) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Nokta Bilgileri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            pointName,
                            { pointName = it },
                            label = { Text("Nokta Adı") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                        )

                        OutlinedTextField(
                            pointDescription,
                            { pointDescription = it },
                            label = { Text("Açıklama") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            leadingIcon = { Icon(Icons.Default.Description, null) }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                east,
                                { east = it },
                                label = { Text("Easting (X)") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Straighten,null) }
                            )

                            OutlinedTextField(
                                north,
                                { north = it },
                                label = { Text("Northing (Y)") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Straighten,null) }
                            )

                            OutlinedTextField(
                                height,
                                { height = it },
                                label = { Text("H (ops)") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Height,null) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scannedCode = ""
                                    pointName = ""
                                    pointDescription = ""
                                    east=""
                                    north=""
                                    height=""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Temizle")
                            }

                            Button(
                                onClick = {
                                    val e = east.replace(',','.').toDoubleOrNull()
                                    val n = north.replace(',','.').toDoubleOrNull()
                                    val h = height.replace(',','.').toDoubleOrNull()
                                    vm.savePoint(pointName.ifBlank { scannedCode }, pointDescription.ifBlank { null }, e, n, h)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kaydet")
                            }
                        }
                    }
                }
            }

            // Geçmiş Barkodlar
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Son Okunan Barkodlar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val recentCodes = listOf("P1234", "P5678", "P9012", "P3456")

                    recentCodes.forEach { code ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(20.dp))
                                    Text(code)
                                }

                                TextButton(onClick = {
                                    scannedCode = code
                                    pointName = "Nokta_$code"
                                }) {
                                    Text("Kullan")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
