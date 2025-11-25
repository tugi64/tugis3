package com.example.tugis3.ui.stakeout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme

class MainStakeoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                StakeoutScreen()
            }
        }
    }
}

@Composable
fun StakeoutScreen() {
    var selectedPoint by remember { mutableStateOf<PointData?>(null) }
    val pointList = remember {
        listOf(
            PointData(1, "Nokta 1", 456789.12, 123456.34, 32.5),
            PointData(2, "Nokta 2", 456790.00, 123457.00, 33.0),
            PointData(3, "Nokta 3", 456791.50, 123458.20, 31.8)
        )
    }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = pointList.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Stakeout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showSearch = !showSearch },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (showSearch) "Kapat" else "Nokta Ara")
        }
        if (showSearch) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Nokta Adı ile Ara") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            filteredList.forEach { point ->
                Button(
                    onClick = { selectedPoint = point; showSearch = false },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = point.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Nokta seçilince otomatik aplikasyon
        selectedPoint?.let { point ->
            Text(text = "Aplikasyon Başlatıldı!", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Seçilen Nokta: ${point.name}")
            Text(text = "N: ${point.northing}")
            Text(text = "E: ${point.easting}")
            Text(text = "Z: ${point.height}")
        }
    }
}

// Nokta veri modeli
data class PointData(
    val id: Int,
    val name: String,
    val northing: Double,
    val easting: Double,
    val height: Double
)
