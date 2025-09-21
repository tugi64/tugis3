package com.example.tugis3.ui.tools

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

class CogoToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                CogoToolsScreen()
            }
        }
    }
}

@Composable
fun CogoToolsScreen() {
    var selectedTool by remember { mutableStateOf("Coordinate Inverse") }
    
    val cogoTools = listOf(
        CogoTool("Coordinate Inverse", "Calculate distance and azimuth between two points", Icons.Default.ArrowForward),
        CogoTool("Offset Distance/Angle", "Calculate offset point coordinates", Icons.Default.ArrowBack),
        CogoTool("Spatial Distance", "Calculate 3D distance between points", Icons.Default.Height),
        CogoTool("Angle Calculation", "Calculate angles between three points", Icons.Default.Angle),
        CogoTool("Intersection", "Calculate intersection of two lines", Icons.Default.CallMerge),
        CogoTool("Resection", "Calculate point coordinates from known lines", Icons.Default.LocationSearching),
        CogoTool("Forward Intersection", "Calculate intersection from angles", Icons.Default.ArrowUpward),
        CogoTool("Coordinate Traverse", "Calculate traverse point coordinates", Icons.Default.Timeline),
        CogoTool("Offset Point", "Calculate offset point from line", Icons.Default.CallSplit),
        CogoTool("Divide Line Equally", "Divide line into equal segments", Icons.Default.Divide),
        CogoTool("Circle Center", "Calculate circle center from three points", Icons.Default.RadioButtonUnchecked),
        CogoTool("Traverse Calculation", "Calculate traverse with 2 points direction", Icons.Default.Navigation)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("COGO Tools") },
                actions = {
                    IconButton(onClick = { /* Points Database */ }) {
                        Icon(Icons.Default.Storage, contentDescription = "Points Database")
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
            // COGO Tools List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cogoTools) { tool ->
                    CogoToolCard(
                        tool = tool,
                        isSelected = selectedTool == tool.name,
                        onClick = { selectedTool = tool.name }
                    )
                }
            }
            
            // Selected Tool Content
            when (selectedTool) {
                "Coordinate Inverse" -> CoordinateInverseContent()
                "Offset Distance/Angle" -> OffsetDistanceAngleContent()
                "Spatial Distance" -> SpatialDistanceContent()
                "Angle Calculation" -> AngleCalculationContent()
                "Intersection" -> IntersectionContent()
                "Resection" -> ResectionContent()
                "Forward Intersection" -> ForwardIntersectionContent()
                "Coordinate Traverse" -> CoordinateTraverseContent()
                "Offset Point" -> OffsetPointContent()
                "Divide Line Equally" -> DivideLineEquallyContent()
                "Circle Center" -> CircleCenterContent()
                "Traverse Calculation" -> TraverseCalculationContent()
            }
        }
    }
}

@Composable
fun CogoToolCard(
    tool: CogoTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
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
                imageVector = tool.icon,
                contentDescription = tool.name,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CoordinateInverseContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Coordinate Inverse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate horizontal distance, azimuth, height difference, slope ratio and slope distance between two points.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start Point A
            Text(
                text = "Start Point A",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Northing") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Easting") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End Point B
            Text(
                text = "End Point B",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Northing") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Easting") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Calculate */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculate")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculate")
                }
                
                Button(
                    onClick = { /* Save to Points Database */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Horizontal Distance:")
                        Text("123.456 m")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Azimuth:")
                        Text("45°30'15\"")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Height Difference:")
                        Text("5.678 m")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slope Ratio:")
                        Text("1:21.7")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slope Distance:")
                        Text("123.687 m")
                    }
                }
            }
        }
    }
}

@Composable
fun OffsetDistanceAngleContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Offset Distance/Angle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate offset point coordinates from a line defined by two points.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Implementation for Offset Distance/Angle
            Text("This tool calculates offset point coordinates...")
        }
    }
}

@Composable
fun SpatialDistanceContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spatial Distance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate 3D spatial distance between two points.")
        }
    }
}

@Composable
fun AngleCalculationContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Angle Calculation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate angles between three points.")
        }
    }
}

@Composable
fun IntersectionContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Intersection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate intersection point of two lines.")
        }
    }
}

@Composable
fun ResectionContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate point coordinates from known lines.")
        }
    }
}

@Composable
fun ForwardIntersectionContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Forward Intersection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate intersection point from angles.")
        }
    }
}

@Composable
fun CoordinateTraverseContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Coordinate Traverse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate traverse point coordinates.")
        }
    }
}

@Composable
fun OffsetPointContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Offset Point",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate offset point from line.")
        }
    }
}

@Composable
fun DivideLineEquallyContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Divide Line Equally",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Divide line into equal segments.")
        }
    }
}

@Composable
fun CircleCenterContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Circle Center",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate circle center from three points.")
        }
    }
}

@Composable
fun TraverseCalculationContent() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Traverse Calculation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Calculate traverse with 2 points direction.")
        }
    }
}

data class CogoTool(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
