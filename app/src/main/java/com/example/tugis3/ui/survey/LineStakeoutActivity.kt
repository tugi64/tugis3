package com.example.tugis3.ui.survey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs
import com.example.tugis3.ui.common.SectionCard
import kotlin.math.min
import kotlin.math.sqrt

@AndroidEntryPoint
class LineStakeoutActivity : ComponentActivity() {
    private val vm: LineStakeoutViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) vm.startEngine()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { LineStakeoutScreen(onBack = { finish() }) } }
        ensurePermissions()
    }

    override fun onDestroy() {
        vm.stopEngine(); super.onDestroy()
    }

    private fun ensurePermissions() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        }
        val ask = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (ask.isNotEmpty()) permissionLauncher.launch(ask.toTypedArray()) else vm.startEngine()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineStakeoutScreen(onBack: () -> Unit, vm: LineStakeoutViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by vm.state.collectAsState()
    val points by vm.projectPoints.collectAsState()
    val lineName by vm.lineName.collectAsState()
    val interval by vm.stakeInterval.collectAsState()
    val latTol by vm.lateralTolerance.collectAsState()
    val chainTol by vm.chainTolerance.collectAsState()
    val simulate by vm.simulate.collectAsState()
    val simLat by vm.simLat.collectAsState()
    val simChain by vm.simChain.collectAsState()
    val project by vm.activeProject.collectAsState()

    var manualStartE by remember { mutableStateOf("") }
    var manualStartN by remember { mutableStateOf("") }
    var manualEndE by remember { mutableStateOf("") }
    var manualEndN by remember { mutableStateOf("") }
    var pointFilter by remember { mutableStateOf("") }
    var showPointSelect by remember { mutableStateOf<PointSelectType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hat Aplikasyonu") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LineDefinitionCard(
                lineName = lineName,
                onName = vm::setLineName,
                manualStartE = manualStartE,
                manualStartN = manualStartN,
                manualEndE = manualEndE,
                manualEndN = manualEndN,
                onManualChange = { se, sn, ee, en -> manualStartE = se; manualStartN = sn; manualEndE = ee; manualEndN = en },
                onApplyManualStart = {
                    val e = manualStartE.toDoubleOrNull(); val n = manualStartN.toDoubleOrNull(); if (e!=null && n!=null) vm.setManualStart(e,n)
                },
                onApplyManualEnd = {
                    val e = manualEndE.toDoubleOrNull(); val n = manualEndN.toDoubleOrNull(); if (e!=null && n!=null) vm.setManualEnd(e,n)
                },
                onPickStart = { showPointSelect = PointSelectType.Start },
                onPickEnd = { showPointSelect = PointSelectType.End },
                onClear = vm::clearLine
            )

            IntervalToleranceCard(
                interval = interval,
                onInterval = vm::setStakeInterval,
                latTol = latTol,
                onLatTol = vm::setLateralTol,
                chainTol = chainTol,
                onChainTol = vm::setChainTol
            )

            GeometryCard(state, latTol, chainTol)
            NearestStationDirection(state)

            SimulationLineCard(simulate, simLat, simChain, onToggle = { vm.toggleSim() }, onLat = { vm.setSimLat(it) }, onChain = { vm.setSimChain(it) })

            StationsPreviewCard(state, interval)

            StakeCanvas(state, project)

            ActionRowLine(state, onSave = vm::saveStakeIfWithin)
        }
    }

    showPointSelect?.let { mode ->
        PointSelectDialogLine(
            title = if (mode==PointSelectType.Start) "Başlangıç Noktası" else "Bitiş Noktası",
            points = points.filter { if (pointFilter.isBlank()) true else it.name.contains(pointFilter, true) },
            total = points.size,
            filter = pointFilter,
            onFilter = { pointFilter = it },
            onDismiss = { showPointSelect = null },
            onSelect = { name -> if (mode==PointSelectType.Start) vm.selectStart(name) else vm.selectEnd(name); showPointSelect = null }
        )
    }
}

enum class PointSelectType { Start, End }

@Composable
private fun LineDefinitionCard(
    lineName: String,
    onName: (String)->Unit,
    manualStartE: String,
    manualStartN: String,
    manualEndE: String,
    manualEndN: String,
    onManualChange:(String,String,String,String)->Unit,
    onApplyManualStart:()->Unit,
    onApplyManualEnd:()->Unit,
    onPickStart:()->Unit,
    onPickEnd:()->Unit,
    onClear:()->Unit
){
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hat Tanımı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(lineName, onName, label={ Text("Hat Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        Text("Başlangıç", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(manualStartE, { onManualChange(it, manualStartN, manualEndE, manualEndN) }, label={ Text("E") }, singleLine=true, modifier=Modifier.weight(1f))
            OutlinedTextField(manualStartN, { onManualChange(manualStartE, it, manualEndE, manualEndN) }, label={ Text("N") }, singleLine=true, modifier=Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onPickStart, label = { Text("Listeden Seç") })
            AssistChip(onClick = onApplyManualStart, enabled = manualStartE.toDoubleOrNull()!=null && manualStartN.toDoubleOrNull()!=null, label = { Text("Manuel Uygula") })
        }
        HorizontalDivider()
        Text("Bitiş", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(manualEndE, { onManualChange(manualStartE, manualStartN, it, manualEndN) }, label={ Text("E") }, singleLine=true, modifier=Modifier.weight(1f))
            OutlinedTextField(manualEndN, { onManualChange(manualStartE, manualStartN, manualEndE, it) }, label={ Text("N") }, singleLine=true, modifier=Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onPickEnd, label = { Text("Listeden Seç") })
            AssistChip(onClick = onApplyManualEnd, enabled = manualEndE.toDoubleOrNull()!=null && manualEndN.toDoubleOrNull()!=null, label = { Text("Manuel Uygula") })
            AssistChip(onClick = onClear, label = { Text("Temizle") })
        }
    } }
}

@Composable
private fun IntervalToleranceCard(
    interval: Double,
    onInterval:(Double)->Unit,
    latTol: Double,
    onLatTol:(Double)->Unit,
    chainTol: Double,
    onChainTol:(Double)->Unit
){
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Parametreler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(interval.toString(), { it.toDoubleOrNull()?.let(onInterval) }, label={ Text("Aralık (m)") }, singleLine=true, modifier=Modifier.weight(1f))
            OutlinedTextField(latTol.toString(), { it.toDoubleOrNull()?.let(onLatTol) }, label={ Text("Lateral Tol (m)") }, singleLine=true, modifier=Modifier.weight(1f))
            OutlinedTextField(chainTol.toString(), { it.toDoubleOrNull()?.let(onChainTol) }, label={ Text("Chain Tol (m)") }, singleLine=true, modifier=Modifier.weight(1f))
        }
    } }
}

@Composable
private fun GeometryCard(st: LineStakeoutViewModel.LineStakeoutState, latTol: Double, chainTol: Double){
    SectionCard(title = "Geometri / Konum") {
        if (st.observation==null) { Text("GNSS verisi bekleniyor..."); return@SectionCard }
        if (st.lineLength==null) { Text("Hat tanımlı değil veya eksik."); return@SectionCard }
        Text("Hat Uzunluğu: ${String.format("%.3f m", st.lineLength)}")
        Text("Chain: ${st.chain?.let{String.format("%.3f", it)} ?: '-'} / Ofset: ${st.offset?.let{String.format("%.3f", it)} ?: '-'} m")
        Text("Hat Azimutu: ${st.bearingLineDeg?.let{String.format("%.1f°", it)} ?: '-'}  Son Nokta Azimutu: ${st.bearingToEndDeg?.let{String.format("%.1f°", it)} ?: '-'}")
        val latStatus = st.offset?.let { if (abs(it)<=latTol) "OK" else ">"+latTol } ?: "-"
        val chainStatus = st.nearestStation?.let { ns -> st.chain?.let { c -> if (abs(ns.chain - c)<=chainTol) "OK" else ">"+chainTol } } ?: "-"
        Text("Lateral Tolerans: $latStatus  |  Chain Tolerans: $chainStatus")
        if (st.lateralWithin && st.chainWithin) Text("✅ İSTASYON KABUL", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NearestStationDirection(st: LineStakeoutViewModel.LineStakeoutState){
    val obs = st.observation ?: return
    val target = st.nearestStation ?: return
    if (st.lineLength == null) return

    // MaterialTheme değerlerine Canvas scope dışında erişip değişkenlere atayarak sorun çözülecek
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha=0.15f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("En Yakın İstasyon Yönü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val bearing = st.bearingLineDeg ?: return@Column
        // Basit ok: line bearing ve mevcut konumdan istasyona göre delta chain göstergesi
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val center = Offset(size.width/2, size.height/2)
            val radius = size.minDimension * 0.35f
            drawCircle(color = outlineColor, radius = radius, center = center)
            rotate(bearing.toFloat()) {
                val shaft = radius*0.85f
                val tip = Offset(center.x, center.y - shaft)
                val left = Offset(center.x - shaft*0.12f, center.y - shaft*0.65f)
                val right = Offset(center.x + shaft*0.12f, center.y - shaft*0.65f)
                drawLine(primaryColor, center, tip, strokeWidth = 6f)
                drawLine(primaryColor, left, tip, strokeWidth = 6f)
                drawLine(primaryColor, right, tip, strokeWidth = 6f)
            }
        }
        val delta = st.nearestStation?.let { ns -> st.chain?.let { c -> ns.chain - c } }
        Text("ΔChain: ${delta?.let { String.format(Locale.US, "%.2f m", it) } ?: '-'}")
    } }
}

@Composable
private fun SimulationLineCard(enabled:Boolean, dLat:Double, dChain:Double, onToggle:()->Unit, onLat:(Double)->Unit, onChain:(Double)->Unit){
    SectionCard(title = "Simülasyon", actions = {
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }) {
        Column {
            Text("Lateral (ofset) ve zinciraj için sanal düzeltmeler uygular.", style = MaterialTheme.typography.bodySmall)
            if (enabled) {
                Text("ΔOfset: ${String.format("%.2f", dLat)} m  ΔChain: ${String.format("%.2f", dChain)} m", style = MaterialTheme.typography.labelMedium)
                Text("Lateral")
                Slider(value = dLat.toFloat(), onValueChange = { onLat(it.toDouble()) }, valueRange = -5f..5f, steps = 19)
                Text("Chain")
                Slider(value = dChain.toFloat(), onValueChange = { onChain(it.toDouble()) }, valueRange = -10f..10f, steps = 39)
            }
        }
    }
}

@Composable
private fun StationsPreviewCard(st: LineStakeoutViewModel.LineStakeoutState, interval: Double){
    if (st.stations.isEmpty()) return
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("İstasyonlar (Aralık: ${interval} m)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val nearest = st.nearestStation
        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
            items(st.stations) { s ->
                val isNearest = nearest?.chain == s.chain
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(String.format("%.1f m", s.chain), fontWeight = if (isNearest) FontWeight.Bold else FontWeight.Normal)
                        Text("E:${String.format("%.2f", s.e)} N:${String.format("%.2f", s.n)}")
                    }
                    HorizontalDivider()
                }
            }
        }
    } }
}

@Composable
private fun StakeCanvas(st: LineStakeoutViewModel.LineStakeoutState, project: com.example.tugis3.data.db.entity.ProjectEntity?){
    if (st.lineLength==null || st.line?.valid != true) return

    // Renkleri canvas dışında tanımlayalım
    val blueColor = Color.Blue
    val yellowColor = Color.Yellow
    val magentaColor = Color.Magenta

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Hat Görünümü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val stations = st.stations
            val obs = st.observation
            Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                val margin = 32f
                val w = size.width - 2*margin
                val h = size.height - 2*margin
                val sE = st.line.startE()!!; val sN = st.line.startN()!!; val eE = st.line.endE()!!; val eN = st.line.endN()!!
                val minE = minOf(sE, eE); val maxE = maxOf(sE, eE)
                val minN = minOf(sN, eN); val maxN = maxOf(sN, eN)
                val spanE = (maxE-minE).coerceAtLeast(1.0); val spanN = (maxN-minN).coerceAtLeast(1.0)
                val scale = min(w/spanE.toFloat(), h/spanN.toFloat())
                fun map(e:Double,n:Double):Offset { val x = margin + ((e-minE).toFloat()*scale); val y = size.height - margin - ((n-minN).toFloat()*scale); return Offset(x,y) }
                // Hat
                drawLine(blueColor, map(sE,sN), map(eE,eN), strokeWidth = 4f)
                // İstasyonlar
                stations.forEach { stn -> drawCircle(yellowColor, 5f, map(stn.e, stn.n)) }
                // Mevcut konum (projeksiyon)
                obs?.let { o ->
                    if (o.latDeg != null && o.lonDeg != null) {
                        val transformer = ProjectionEngine.forProject(project)
                        val (curE, curN) = if (transformer !== NoOpTransformer) transformer.forward(o.latDeg, o.lonDeg) else ((o.lonDeg * 111000) to (o.latDeg * 111000))
                        drawCircle(magentaColor, 7f, map(curE, curN))
                    }
                }
            }
            Text("Mavi: Hat  |  Sarı: İstasyonlar  |  Magenta: Mevcut", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionRowLine(state: LineStakeoutViewModel.LineStakeoutState, onSave: ()->Unit){
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Route,null); Spacer(Modifier.width(4.dp)); Text("Oto İlerle (yakında)")
        }
        Button(onClick = onSave, enabled = state.lateralWithin && state.chainWithin, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Save,null); Spacer(Modifier.width(4.dp)); Text("Stake Kaydet")
        }
    }
}

@Composable
private fun PointSelectDialogLine(
    title:String,
    points: List<com.example.tugis3.data.db.entity.PointEntity>,
    total:Int,
    filter:String,
    onFilter:(String)->Unit,
    onDismiss:()->Unit,
    onSelect:(String)->Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title ($total)") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(filter, onFilter, label={ Text("Ara") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                if (points.isEmpty()) Text("Sonuç yok") else LazyColumn(Modifier.heightIn(max=340.dp)) {
                    items(points) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text("E:${String.format("%.2f", p.easting)} N:${String.format("%.2f", p.northing)}") },
                            leadingContent = { Icon(Icons.Filled.GpsFixed,null) },
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(p.name) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}
