package com.example.tugis3.ui.device

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

class DeviceCommunicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                DeviceCommunicationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCommunicationScreen() {
    var selectedManufacturer by remember { mutableStateOf("SOUTH") }
    var selectedMode by remember { mutableStateOf("Bluetooth") }
    var isScanning by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    
    val bluetoothDevices = remember {
        listOf(
            BluetoothDevice("N80T-123456", "SOUTH N80T", "Available"),
            BluetoothDevice("N80T-789012", "SOUTH N80T", "Available"),
            BluetoothDevice("S86-345678", "SOUTH S86", "Available")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Communication") },
                actions = {
                    IconButton(onClick = { debugMode = !debugMode }) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Debug Mode",
                            tint = if (debugMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
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
            // Manufacturer Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Model",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedManufacturer,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Manufacturer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("SOUTH", "Trimble", "Leica", "Topcon").forEach { manufacturer ->
                                DropdownMenuItem(
                                    text = { Text(manufacturer) },
                                    onClick = {
                                        selectedManufacturer = manufacturer
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Communication Mode Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Communication Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { selectedMode = "Bluetooth" },
                            label = { Text("Bluetooth") },
                            selected = selectedMode == "Bluetooth",
                            leadingIcon = {
                                Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth")
                            }
                        )
                        
                        FilterChip(
                            onClick = { selectedMode = "WLAN" },
                            label = { Text("WLAN") },
                            selected = selectedMode == "WLAN",
                            leadingIcon = {
                                Icon(Icons.Default.Wifi, contentDescription = "WLAN")
                            }
                        )
                        
                        FilterChip(
                            onClick = { selectedMode = "Demo" },
                            label = { Text("Demo") },
                            selected = selectedMode == "Demo",
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Demo")
                            }
                        )
                    }
                }
            }
            
            // Device List (Bluetooth Mode)
            if (selectedMode == "Bluetooth") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Devices",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Button(
                                onClick = { isScanning = !isScanning },
                                enabled = !isScanning
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isScanning) "Scanning..." else "Scan")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bluetoothDevices) { device ->
                                BluetoothDeviceCard(
                                    device = device,
                                    onConnect = { /* Connect to device */ },
                                    isConnected = isConnected
                                )
                            }
                        }
                    }
                }
            }
            
            // WLAN Mode
            if (selectedMode == "WLAN") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "WLAN Connection",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Connect to receiver's WiFi hotspot")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { /* Connect to WLAN */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = "WLAN")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect to Receiver WLAN")
                        }
                    }
                }
            }
            
            // Demo Mode
            if (selectedMode == "Demo") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Demo Mode",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Use SurvStar without connecting to real receiver")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Starting Point Coordinates") },
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
                                label = { Text("Direction") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "",
                                onValueChange = { },
                                label = { Text("Speed") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Debug Mode
            if (debugMode) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Debug Mode",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* Start/Stop data stream */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start/Stop")
                            }
                            
                            Button(
                                onClick = { /* Send command */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send")
                            }
                            
                            Button(
                                onClick = { /* Clear */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Send Command") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Command List:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "• $GPGGA - Position data\n• $GPGSA - Satellite status\n• $GPGSV - Satellite visibility",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceCard(
    device: BluetoothDevice,
    onConnect: () -> Unit,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onConnect,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Device",
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.status == "Available") 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isConnected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class BluetoothDevice(
    val serialNumber: String,
    val model: String,
    val status: String
) {
    val name: String get() = serialNumber
}
