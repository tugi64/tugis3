package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoordinateSystemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CoordinateSystemScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordinateSystemScreen(onBack: () -> Unit) {
    var selectedCountry by remember { mutableStateOf("TURKEY") }
    var selectedSystem by remember { mutableStateOf("WGS84/TM33") }
    var customParameters by remember { mutableStateOf(CoordinateParameters()) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val countries = listOf("TURKEY", "SWITZERLAND", "TUNISIA", "UNITED ARAB EMIRATES", "UNITED KINGDOM")
    val turkeyCoordinateSystems = listOf(
        CoordinateSystemInfo("WGS84/TM27", "WGS84", "27° meridyeni"),
        CoordinateSystemInfo("WGS84/TM30", "WGS84", "30° meridyeni"),
        CoordinateSystemInfo("WGS84/TM33", "WGS84", "33° meridyeni"),
        CoordinateSystemInfo("WGS84/TM36", "WGS84", "36° meridyeni"),
        CoordinateSystemInfo("WGS84/TM39", "WGS84", "39° meridyeni"),
        CoordinateSystemInfo("WGS84/TM42", "WGS84", "42° meridyeni"),
        CoordinateSystemInfo("WGS84/TM45", "WGS84", "45° meridyeni"),
        CoordinateSystemInfo(
            "ITRF96/TM30",
            "GRS 1980",
            "30° meridyeni (EPSG:7932)"
        ),
        CoordinateSystemInfo("ED50/UTM zone 35", "International", "UTM Bölge 35"),
        CoordinateSystemInfo("ED50/UTM zone 36", "International", "UTM Bölge 36"),
        CoordinateSystemInfo("ED50/UTM zone 37", "International", "UTM Bölge 37"),
        CoordinateSystemInfo("ED50/UTM zone 38", "International", "UTM Bölge 38")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koordinat Sistemi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Özel Parametreler")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Öntanımlı Koordinat Sistemleri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Ülke Seçimi
                        var countryExpanded by remember { mutableStateOf(false) }
                        // ExposedDropdownMenu yerine stable alternatif
                        Box {
                            OutlinedTextField(
                                value = selectedCountry,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ülke") },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { countryExpanded = !countryExpanded }
                            )

                            DropdownMenu(
                                expanded = countryExpanded,
                                onDismissRequest = { countryExpanded = false }
                            ) {
                                countries.forEach { country ->
                                    DropdownMenuItem(
                                        text = { Text(country) },
                                        onClick = {
                                            selectedCountry = country
                                            countryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Koordinat sistemleri listesi
            if (selectedCountry == "TURKEY") {
                items(turkeyCoordinateSystems) { system ->
                    CoordinateSystemCard(
                        system = system,
                        isSelected = selectedSystem == system.name,
                        onSelect = {
                            selectedSystem = system.name
                            scope.launch {
                                snackbarHostState.showSnackbar("${system.name} seçildi")
                            }
                        }
                    )
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Seçili Sistem Bilgileri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val selectedSystemInfo = turkeyCoordinateSystems.find { it.name == selectedSystem }
                        selectedSystemInfo?.let { system ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoRow("Koordinat Sistemi", system.name)
                                InfoRow("Elipsoid", system.ellipsoid)
                                InfoRow("Açıklama", system.description)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Koordinat sistemi kaydedildi")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kaydet")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Parametreler dışa aktarıldı")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dışa Aktar")
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Geoid Dosyası",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Geoid Düzeltmesi")
                            Switch(
                                checked = true,
                                onCheckedChange = { }
                            )
                        }

                        OutlinedTextField(
                            value = "TG99A.bin",
                            onValueChange = {},
                            label = { Text("Geoid Dosyası") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Dosya Seç")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomParametersDialog(
            parameters = customParameters,
            onDismiss = { showCustomDialog = false },
            onSave = { params ->
                customParameters = params
                showCustomDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Özel parametreler kaydedildi")
                }
            }
        )
    }
}

@Composable
private fun CoordinateSystemCard(
    system: CoordinateSystemInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = system.ellipsoid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = system.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CustomParametersDialog(
    parameters: CoordinateParameters,
    onDismiss: () -> Unit,
    onSave: (CoordinateParameters) -> Unit
) {
    var params by remember { mutableStateOf(parameters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Özel Koordinat Parametreleri") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = params.centralMeridian,
                    onValueChange = { params = params.copy(centralMeridian = it) },
                    label = { Text("Merkezi Meridyen (°)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = params.falseEasting,
                    onValueChange = { params = params.copy(falseEasting = it) },
                    label = { Text("Yanlış Doğu (m)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = params.falseNorthing,
                    onValueChange = { params = params.copy(falseNorthing = it) },
                    label = { Text("Yanlış Kuzey (m)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = params.scaleFactor,
                    onValueChange = { params = params.copy(scaleFactor = it) },
                    label = { Text("Ölçek Faktörü") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(params) }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

data class CoordinateSystemInfo(
    val name: String,
    val ellipsoid: String,
    val description: String
)

data class CoordinateParameters(
    val centralMeridian: String = "33.0",
    val falseEasting: String = "500000.0",
    val falseNorthing: String = "0.0",
    val scaleFactor: String = "0.9996"
)
