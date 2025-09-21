package com.example.tugis3.ui.survey

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

class PointSurveyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                PointSurveyScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointSurveyScreen() {
    var isRecording by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf("No location") }
    var accuracy by remember { mutableStateOf("--") }
    var satelliteCount by remember { mutableStateOf(0) }
    var gnssStatus by remember { mutableStateOf("Single") }
    var hrms by remember { mutableStateOf("2.5") }
    var vrms by remember { mutableStateOf("3.1") }
    var workMode by remember { mutableStateOf("Rover") }
    var datalink by remember { mutableStateOf("UHF") }
    var battery by remember { mutableStateOf(85) }
    
    Scaffold(
        topBar = {
            // SurvStar benzeri üst bilgi çubuğu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // GNSS Status ve Satellite Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Satellite,
                                contentDescription = "GNSS Status",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = gnssStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "12/15",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "H:${hrms}m V:${vrms}m",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Work Mode, Datalink, Battery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = workMode,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = datalink,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Battery: ${battery}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(
                            onClick = { /* SAT Information */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "SAT Info",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isRecording = !isRecording },
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sol Toolbar - SurvStar benzeri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Map Display
                IconButton(onClick = { /* Map Display */ }) {
                    Icon(Icons.Default.Map, contentDescription = "Map Display")
                }
                
                // Auto Map Center
                IconButton(onClick = { /* Auto Map Center */ }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Auto Map Center")
                }
                
                // Map Enlarge/Reduce
                IconButton(onClick = { /* Map Enlarge */ }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Map Enlarge")
                }
                
                IconButton(onClick = { /* Map Reduce */ }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Map Reduce")
                }
                
                // Antenna Parameter
                IconButton(onClick = { /* Antenna Parameter */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Antenna Parameter")
                }
                
                // Screen Survey
                IconButton(onClick = { /* Screen Survey */ }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Screen Survey")
                }
            }
            
            // Sağ Toolbar - SurvStar benzeri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Graphic Plot
                IconButton(onClick = { /* Graphic Plot */ }) {
                    Icon(Icons.Default.Polyline, contentDescription = "Graphic Plot")
                }
                
                // Point Type Selector
                IconButton(onClick = { /* Point Type */ }) {
                    Icon(Icons.Default.Category, contentDescription = "Point Type")
                }
            }
            
            // Point Information Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Point Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        label = { Text("Point Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        label = { Text("Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Antenna Height") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Type") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Record Settings Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Record Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Average GPS Recording Count:")
                        Text("1")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Record Limit:")
                        Text("HRMS: 0.05m, VRMS: 0.08m")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PDOP Limit:")
                        Text("6.0")
                    }
                }
            }
            
            // Recent Points List
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Recent Points",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("P1", "P2", "P3", "P4", "P5")) { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(point)
                                Text("N: 123456.789 E: 987654.321 H: 45.678")
                            }
                        }
                    }
                }
            }
        }
    }
}
