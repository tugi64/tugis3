package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

class DistanceCalculationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DistanceCalcScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistanceCalcScreen() {
    var x1 by remember { mutableStateOf("") }
    var y1 by remember { mutableStateOf("") }
    var z1 by remember { mutableStateOf("") }
    var x2 by remember { mutableStateOf("") }
    var y2 by remember { mutableStateOf("") }
    var z2 by remember { mutableStateOf("") }
    var result2D by remember { mutableStateOf<String?>(null) }
    var result3D by remember { mutableStateOf<String?>(null) }

    fun parse(v: String) = v.replace(',', '.').toDoubleOrNull()

    fun calculate() {
        val X1 = parse(x1); val Y1 = parse(y1); val Z1 = parse(z1)
        val X2 = parse(x2); val Y2 = parse(y2); val Z2 = parse(z2)
        if (X1==null || Y1==null || X2==null || Y2==null) {
            result2D = null; result3D = null; return
        }
        val dx = X2 - X1
        val dy = Y2 - Y1
        val d2 = sqrt(dx*dx + dy*dy)
        result2D = String.format("2B Mesafe: %.4f m", d2)
        if (Z1!=null && Z2!=null) {
            val dz = Z2 - Z1
            val d3 = sqrt(dx.pow(2)+dy.pow(2)+dz.pow(2))
            result3D = String.format("3B Mesafe: %.4f m (dZ=%.4f)", d3, dz)
        } else result3D = null
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Uzaklık Hesabı") }) }) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nokta 1", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(x1, { x1 = it }, label = { Text("X1") }, modifier = Modifier.weight(1f))
                OutlinedTextField(y1, { y1 = it }, label = { Text("Y1") }, modifier = Modifier.weight(1f))
                OutlinedTextField(z1, { z1 = it }, label = { Text("Z1") }, modifier = Modifier.weight(1f))
            }
            Text("Nokta 2", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(x2, { x2 = it }, label = { Text("X2") }, modifier = Modifier.weight(1f))
                OutlinedTextField(y2, { y2 = it }, label = { Text("Y2") }, modifier = Modifier.weight(1f))
                OutlinedTextField(z2, { z2 = it }, label = { Text("Z2") }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { calculate() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text("Hesapla")
                }
                OutlinedButton(onClick = {
                    x1=""; y1=""; z1=""; x2=""; y2=""; z2=""; result2D=null; result3D=null
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, contentDescription=null); Spacer(Modifier.width(6.dp)); Text("Temizle") }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sonuçlar", fontWeight = FontWeight.Bold)
                    Text(result2D ?: "En az X/Y değerlerini girin")
                    if (result3D != null) Text(result3D!!) else Text("3B için Z1/Z2 girin")
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Not: Virgül yerine nokta kullanabilir veya virgül girerseniz otomatik dönüştürülür.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

