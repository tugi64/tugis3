@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.tugis3.ui.theme.Tugis3Theme

@AndroidEntryPoint
class ProjectManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { ProjectManagerScreen() } }
    }
}

@Composable
fun ProjectManagerScreen(vm: ProjectManagerViewModel = hiltViewModel()) {
    val projects by vm.projects.collectAsState()
    val active by vm.activeProject.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, activate ->
                vm.create(name.trim(), desc.trim().ifBlank { null }, activate)
                showCreateDialog = false
            }
        )
    }

    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Projeyi Sil") },
            text = { Text("Bu proje ve noktaları silinecek. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); showDeleteConfirm = null }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("İptal") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeler") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, null) }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ellipsoid bilgi satırı (aktif proje varsa)
            active?.let { ap ->
                if (!ap.ellipsoidName.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Elipsoid: ${ap.ellipsoidName}") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                    )
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Ara") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )
            val filtered = projects.filter { it.name.contains(searchQuery, true) }
            if (filtered.isEmpty()) {
                Text("Proje yok", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { p ->
                        ProjectRow(
                            name = p.name,
                            isActive = p.id == active?.id,
                            description = p.description,
                            ellipsoidName = p.ellipsoidName,
                            onActivate = { vm.setActive(p.id) },
                            onDelete = { showDeleteConfirm = p.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    name: String,
    isActive: Boolean,
    description: String?,
    ellipsoidName: String?,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(onClick = onActivate) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Star else Icons.Default.Folder,
                contentDescription = null
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, fontWeight = FontWeight.Bold)
                if (!description.isNullOrBlank()) Text(description, style = MaterialTheme.typography.bodySmall)
                if (!ellipsoidName.isNullOrBlank()) {
                    Text(
                        "Ellipsoid: $ellipsoidName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (isActive) Text("Aktif", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, activate: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var activate by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Proje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Ad") })
                OutlinedTextField(desc, { desc = it }, label = { Text("Açıklama (ops)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activate, onCheckedChange = { activate = it })
                    Text("Aktif yap")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, desc, activate) }, enabled = name.isNotBlank()) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
