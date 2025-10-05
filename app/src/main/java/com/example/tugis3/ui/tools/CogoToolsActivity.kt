@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.tugis3.ui.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tugis3.cogo.GeoMath
import com.example.tugis3.ui.theme.Tugis3Theme
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
fun CogoToolsScreen(vm: CogoPrefsViewModel = hiltViewModel()) {
    val tabs = listOf("Inverse", "Doğru", "Projeksiyon", "Açı")
    val selectedTab by vm.tab.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("COGO Tools") },
                actions = { IconButton(onClick = { }) { Icon(Icons.Default.Storage, contentDescription = null) } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = idx == selectedTab, onClick = { vm.setTab(idx) }, text = { Text(title) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tabs[selectedTab]) {
                "Inverse" -> CoordinateInverseContent()
                "Doğru" -> LineFromTwoPointsContent()
                "Projeksiyon" -> PointProjectionContent()
                "Açı" -> AngleSolverContent()
            }
        }
    }
}

@Composable
fun CoordinateInverseContent() {
    // State for inputs
    var nA by remember { mutableStateOf("") }
    var eA by remember { mutableStateOf("") }
    var hA by remember { mutableStateOf("") }
    var nB by remember { mutableStateOf("") }
    var eB by remember { mutableStateOf("") }
    var hB by remember { mutableStateOf("") }

    // Results state
    var horizontal by remember { mutableStateOf<Double?>(null) }
    var azimuth by remember { mutableStateOf<Double?>(null) }
    var dh by remember { mutableStateOf<Double?>(null) }
    var slope by remember { mutableStateOf<Double?>(null) }
    var slopeRatio by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun parse(v: String): Double? = v.trim().replace(',', '.').toDoubleOrNull()

    fun calculate() {
        errorMsg = null
        val n1 = parse(nA); val e1 = parse(eA)
        val n2 = parse(nB); val e2 = parse(eB)
        if (n1 == null || e1 == null || n2 == null || e2 == null) {
            errorMsg = "Geçerli N/E değerleri giriniz"; return
        }
        val h1 = parse(hA)
        val h2 = parse(hB)
        val hDist = GeoMath.horizontalDistance(n1, e1, n2, e2)
        val az = GeoMath.azimuthDeg(n1, e1, n2, e2)
        val deltaH = if (h1 != null && h2 != null) h2 - h1 else null
        val slopeDist = if (deltaH != null) GeoMath.slopeDistance(hDist, deltaH) else null
        val ratio = if (deltaH != null) GeoMath.slopeRatio(hDist, deltaH) else null
        horizontal = hDist
        azimuth = az
        dh = deltaH
        slope = slopeDist
        slopeRatio = ratio
    }

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
            Spacer(modifier = Modifier.height(8.dp))
            Text("İki nokta arasındaki yatay mesafe, azimut, yükseklik farkı ve eğik mesafe hesaplanır.")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Nokta A", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nA, { nA = it }, label = { Text("Northing") }, modifier = Modifier.weight(1f))
                OutlinedTextField(eA, { eA = it }, label = { Text("Easting") }, modifier = Modifier.weight(1f))
                OutlinedTextField(hA, { hA = it }, label = { Text("Height") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nokta B", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nB, { nB = it }, label = { Text("Northing") }, modifier = Modifier.weight(1f))
                OutlinedTextField(eB, { eB = it }, label = { Text("Easting") }, modifier = Modifier.weight(1f))
                OutlinedTextField(hB, { hB = it }, label = { Text("Height") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { calculate() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Hesapla")
                }
                OutlinedButton(onClick = {
                    nA=""; eA=""; hA=""; nB=""; eB=""; hB=""
                    horizontal=null; azimuth=null; dh=null; slope=null; slopeRatio=null; errorMsg=null
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Temizle")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sonuçlar", fontWeight = FontWeight.Bold)
                    Text("Yatay Mesafe: ${horizontal?.let { String.format(Locale.US, "%.4f m", it) } ?: "-"}")
                    Text("Azimut: ${azimuth?.let { GeoMath.dmsFormat(it) } ?: "-"}")
                    Text("Yükseklik Farkı: ${dh?.let { String.format(Locale.US, "%.4f m", it) } ?: "-"}")
                    Text("Eğik Mesafe: ${slope?.let { String.format(Locale.US, "%.4f m", it) } ?: "-"}")
                    Text("Eğim Oranı: ${slopeRatio ?: "-"}")
                }
            }
        }
    }
}

@Composable
fun LineFromTwoPointsContent() {
    var n1 by remember { mutableStateOf("") }; var e1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }; var e2 by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf<Double?>(null) }
    var azimuth by remember { mutableStateOf<Double?>(null) }
    var midN by remember { mutableStateOf<Double?>(null) }; var midE by remember { mutableStateOf<Double?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    fun parse(s: String) = s.trim().replace(',', '.').toDoubleOrNull()
    fun calc() {
        err = null
        val N1 = parse(n1); val E1 = parse(e1); val N2 = parse(n2); val E2 = parse(e2)
        if (N1==null||E1==null||N2==null||E2==null) { err = "Geçerli N/E giriniz"; return }
        distance = GeoMath.horizontalDistance(N1,E1,N2,E2)
        azimuth = GeoMath.azimuthDeg(N1,E1,N2,E2)
        midN = (N1+N2)/2.0; midE = (E1+E2)/2.0
    }
    Card(Modifier.fillMaxWidth().padding(12.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("İki Noktadan Doğru", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("İki nokta arasındaki doğrultu azimut, mesafe ve orta nokta hesaplanır.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(n1,{n1=it}, label={Text("N1")}, modifier=Modifier.weight(1f))
            OutlinedTextField(e1,{e1=it}, label={Text("E1")}, modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(n2,{n2=it}, label={Text("N2")}, modifier=Modifier.weight(1f))
            OutlinedTextField(e2,{e2=it}, label={Text("E2")}, modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick={calc()}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Calculate,null); Spacer(Modifier.width(4.dp)); Text("Hesapla") }
            OutlinedButton(onClick={ n1="";e1="";n2="";e2="";distance=null;azimuth=null;midN=null;midE=null;err=null }, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(4.dp)); Text("Temizle") }
        }
        if (err!=null) Text(err!!, color=MaterialTheme.colorScheme.error)
        Card(colors=CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sonuçlar", fontWeight = FontWeight.Bold)
                Text("Mesafe: ${distance?.let { String.format(Locale.US,"%.4f m",it) } ?: "-"}")
                Text("Azimut: ${azimuth?.let { GeoMath.dmsFormat(it) } ?: "-"}")
                Text("Orta N: ${midN?.let { String.format(Locale.US,"%.4f",it) } ?: "-"}  E: ${midE?.let { String.format(Locale.US,"%.4f",it) } ?: "-"}")
            }
        }
    }}
}

@Composable
fun PointProjectionContent() {
    var n by remember { mutableStateOf("") }; var e by remember { mutableStateOf("") }
    var dist by remember { mutableStateOf("") }; var az by remember { mutableStateOf("") }
    var outN by remember { mutableStateOf<Double?>(null) }; var outE by remember { mutableStateOf<Double?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    fun parse(s:String)=s.trim().replace(',', '.').toDoubleOrNull()
    fun calc() {
        err=null
        val N=parse(n); val E=parse(e); val D=parse(dist); val A=parse(az)
        if (N==null||E==null||D==null||A==null) { err="Geçersiz giriş"; return }
        val (n2,e2)=GeoMath.forwardNE(N,E,D,A)
        outN=n2; outE=e2
    }
    Card(Modifier.fillMaxWidth().padding(12.dp)) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Nokta Projeksiyonu", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Başlangıç noktası, mesafe ve azimut ile hedef koordinat hesaplanır.", style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(n,{n=it}, label={Text("N")}, modifier=Modifier.weight(1f))
            OutlinedTextField(e,{e=it}, label={Text("E")}, modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(dist,{dist=it}, label={Text("Mesafe")}, modifier=Modifier.weight(1f))
            OutlinedTextField(az,{az=it}, label={Text("Azimut (deg)")}, modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={calc()}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Calculate,null); Spacer(Modifier.width(4.dp)); Text("Hesapla") }
            OutlinedButton(onClick={ n="";e="";dist="";az="";outN=null;outE=null;err=null }, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(4.dp)); Text("Temizle") }
        }
        if (err!=null) Text(err!!, color=MaterialTheme.colorScheme.error)
        Card(colors=CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sonuçlar", fontWeight = FontWeight.Bold)
                Text("Hedef N: ${outN?.let { String.format(Locale.US,"%.4f",it) } ?: "-"}")
                Text("Hedef E: ${outE?.let { String.format(Locale.US,"%.4f",it) } ?: "-"}")
            }
        }
    }}
}

@Composable
fun AngleSolverContent() {
    var nA by remember { mutableStateOf("") }; var eA by remember { mutableStateOf("") }
    var nB by remember { mutableStateOf("") }; var eB by remember { mutableStateOf("") }
    var nC by remember { mutableStateOf("") }; var eC by remember { mutableStateOf("") }
    var angle by remember { mutableStateOf<Double?>(null) }
    var azAB by remember { mutableStateOf<Double?>(null) }; var azBC by remember { mutableStateOf<Double?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    fun parse(s:String)=s.trim().replace(',', '.').toDoubleOrNull()
    fun calc() {
        err=null
        val NA=parse(nA); val EA=parse(eA); val NB=parse(nB); val EB=parse(eB); val NC=parse(nC); val EC=parse(eC)
        if (NA==null||EA==null||NB==null||EB==null||NC==null||EC==null){ err="Geçersiz giriş"; return }
        azAB = GeoMath.azimuthDeg(NB,EB,NA,EA)
        azBC = GeoMath.azimuthDeg(NB,EB,NC,EC)
        angle = GeoMath.angleAt(NA,EA,NB,EB,NC,EC)
    }
    Card(Modifier.fillMaxWidth().padding(12.dp)) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Açı Çözümü", style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("A-B-C üçlüsünde B tepe açısı ve yön azimutları.", style=MaterialTheme.typography.bodySmall)
        Text("Nokta A")
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nA,{nA=it}, label={Text("NA")}, modifier=Modifier.weight(1f))
            OutlinedTextField(eA,{eA=it}, label={Text("EA")}, modifier=Modifier.weight(1f))
        }
        Text("Nokta B")
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nB,{nB=it}, label={Text("NB")}, modifier=Modifier.weight(1f))
            OutlinedTextField(eB,{eB=it}, label={Text("EB")}, modifier=Modifier.weight(1f))
        }
        Text("Nokta C")
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nC,{nC=it}, label={Text("NC")}, modifier=Modifier.weight(1f))
            OutlinedTextField(eC,{eC=it}, label={Text("EC")}, modifier=Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={calc()}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Calculate,null); Spacer(Modifier.width(4.dp)); Text("Hesapla") }
            OutlinedButton(onClick={ nA="";eA="";nB="";eB="";nC="";eC="";angle=null;azAB=null;azBC=null;err=null }, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(4.dp)); Text("Temizle") }
        }
        if (err!=null) Text(err!!, color=MaterialTheme.colorScheme.error)
        Card(colors=CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sonuçlar", fontWeight = FontWeight.Bold)
                Text("Azimut AB: ${azAB?.let { GeoMath.dmsFormat(it) } ?: "-"}")
                Text("Azimut BC: ${azBC?.let { GeoMath.dmsFormat(it) } ?: "-"}")
                Text("B Açısı: ${angle?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"}")
            }
        }
    }}
}
