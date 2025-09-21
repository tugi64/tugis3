package com.example.tugis3.ui.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme

class ProjectManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                ProjectManagerScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagerScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    val projects = remember {
        listOf(
            ProjectItem("Survey Project 1", "2024-01-15", "Active"),
            ProjectItem("Road Construction", "2024-01-10", "Inactive"),
            ProjectItem("Building Survey", "2024-01-05", "Inactive"),
            ProjectItem("Land Survey", "2024-01-01", "Inactive")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Manager") },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import Project")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Project")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Projects") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Project List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
                }) { project ->
                    ProjectItemCard(
                        project = project,
                        onOpen = { /* Open project */ },
                        onDelete = { /* Delete project */ },
                        onDetails = { /* Show details */ }
                    )
                }
            }
        }
    }
    
    // Create Project Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { /* Create project */ }
        )
    }
    
    // Import Project Dialog
    if (showImportDialog) {
        ImportProjectDialog(
            onDismiss = { showImportDialog = false },
            onImport = { /* Import project */ }
        )
    }
}

@Composable
fun ProjectItemCard(
    project: ProjectItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Project",
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Created: ${project.createdDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${project.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (project.status == "Active") 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onDetails) {
                    Icon(Icons.Default.Info, contentDescription = "Details")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var coordinateSystem by remember { mutableStateOf("Previous Project Parameters") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = operator,
                    onValueChange = { operator = it },
                    label = { Text("Operator") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = coordinateSystem,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Coordinate System Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(
                            "Previous Project Parameters",
                            "New project without parameters",
                            "RTCM1021~1027 Parameters"
                        ).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    coordinateSystem = option
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
                    onCreate()
                    onDismiss()
                },
                enabled = projectName.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportProjectDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Project") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select project file (*.configure) to import:")
                
                Button(
                    onClick = { /* File picker */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Select File")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose File")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(); onDismiss() }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class ProjectItem(
    val name: String,
    val createdDate: String,
    val status: String
)
