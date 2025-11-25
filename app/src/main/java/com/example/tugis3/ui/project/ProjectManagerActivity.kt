package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProjectManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ProjectManagerScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectManagerScreen(onBack: () -> Unit) {
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf(listOf<Project>()) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    // Örnek proje verileri
    LaunchedEffect(Unit) {
        projects = listOf(
            Project("Proje_2024_01", "Ana Yol Projesi", Date(), "TM33"),
            Project("Kadastro_2024", "Kadastro Çalışması", Date(), "TM30"),
            Project("Tünel_Projesi", "Metro Tüneli", Date(), "TM36")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proje Yöneticisi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Proje")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProjectDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Proje")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    isSelected = selectedProject == project,
                    onSelect = { selectedProject = project },
                    onOpen = {
                        // Projeyi aç
                    },
                    onDelete = {
                        projects = projects.filter { it != project }
                    }
                )
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, description, coordSystem ->
                val newProject = Project(
                    name = name,
                    description = description,
                    createdDate = Date(),
                    coordinateSystem = coordSystem
                )
                projects = projects + newProject
                showNewProjectDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    project: Project,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Koordinat: ${project.coordinateSystem}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            .format(project.createdDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Aç")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coordinateSystem by remember { mutableStateOf("WGS84/TM33") }
    var expanded by remember { mutableStateOf(false) }

    val coordinateSystems = listOf(
        "WGS84/TM27", "WGS84/TM30", "WGS84/TM33", "WGS84/TM36",
        "WGS84/TM39", "WGS84/TM42", "WGS84/TM45",
        "ED50/UTM zone 35", "ED50/UTM zone 36", "ED50/UTM zone 37"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Proje Oluştur") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Proje Adı") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = coordinateSystem,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Koordinat Sistemi") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        coordinateSystems.forEach { system ->
                            DropdownMenuItem(
                                text = { Text(system) },
                                onClick = {
                                    coordinateSystem = system
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, coordinateSystem)
                    }
                }
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

data class Project(
    val name: String,
    val description: String,
    val createdDate: Date,
    val coordinateSystem: String
)
