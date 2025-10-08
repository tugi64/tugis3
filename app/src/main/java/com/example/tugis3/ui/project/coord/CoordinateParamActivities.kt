@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tugis3.ui.project.coord

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Basit parametre depolama (in-memory); TODO: DataStore ile kalıcı hale getir */
object CoordParamsStore { // Artık DataStore destekli
    private val map = mutableStateMapOf<String, String>() // UI anında güncelleme için cache
    fun get(key: String, def: String = ""): String = map[key] ?: def
    fun put(key: String, v: String) { map[key] = v }
}

private val Context.coordParamsDataStore by preferencesDataStore(name = "coord_params")

private suspend fun saveParam(context: Context, key: String, value: String) {
    context.coordParamsDataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    CoordParamsStore.put(key, value)
}

private suspend fun loadParam(context: Context, key: String): String {
    val current = CoordParamsStore.get(key)
    if (current.isNotEmpty()) return current
    val prefs = context.coordParamsDataStore.data.firstOrNullSafe()
    val loaded = prefs?.get(stringPreferencesKey(key)) ?: ""
    if (loaded.isNotEmpty()) CoordParamsStore.put(key, loaded)
    return loaded
}

// Basit, cancellation-safe firstOrNull wrapper
private suspend fun <T> Flow<T>.firstOrNullSafe(): T? = try { firstOrNull() } catch (_: Exception) { null }

private fun genericTitle(id: String): String = when(id) {
    "ProjectionParametersActivity" -> "Projeksiyon Parametreleri"
    "ItrfParametersActivity" -> "ITRF Parametreleri"
    "SevenParamsActivity" -> "Yedi Parametre"
    "FourParamsActivity" -> "Dört Parametre / Yatay Ayar"
    "VerticalControlParametersActivity" -> "Dikey Kontrol Parametreleri"
    "VerticalAdjustmentParametersActivity" -> "Dikey Ayar Parametreleri"
    "GridFileActivity" -> "Grid Dosyası"
    "GeoidFileActivity" -> "Geoid Dosyası"
    "LocalOffsetsActivity" -> "Yerel Ofsetler"
    else -> id
}

// Param tanımı ve doğrulama
private data class ParamDef(
    val key: String,
    val label: String,
    val numeric: Boolean = false,
    val validator: (String) -> String? = { null },
    val showDerived: Boolean = false // rotasyon için radyan gösterimi
)

// Sayısal alanlar için tekrar eden doğrulama fonksiyonu (uyarı temizlendi)
private val numericValidator: (String) -> String? = { v ->
    if (v.isBlank()) null else if (v.toDoubleOrNull() != null) null else "Sayı girin"
}

private val sevenParamDefs = listOf(
    ParamDef(key = "dX", label = "dX (m)", numeric = true, validator = numericValidator),
    ParamDef(key = "dY", label = "dY (m)", numeric = true, validator = numericValidator),
    ParamDef(key = "dZ", label = "dZ (m)", numeric = true, validator = numericValidator),
    ParamDef(key = "rotX", label = "RotX (arcsec)", numeric = true, validator = { validateRotation(it) }, showDerived = true),
    ParamDef(key = "rotY", label = "RotY (arcsec)", numeric = true, validator = { validateRotation(it) }, showDerived = true),
    ParamDef(key = "rotZ", label = "RotZ (arcsec)", numeric = true, validator = { validateRotation(it) }, showDerived = true),
    ParamDef(key = "scale", label = "Scale (ppm)", numeric = true, validator = numericValidator)
)

private fun paramDefinitions(activityId: String): List<ParamDef> = when(activityId) {
    "ProjectionParametersActivity" -> listOf(
        ParamDef(key = "frame", label = "Referans Çerçeve (örn. ITRF96)"),
        ParamDef(key = "ellipsoid", label = "Elipsoid (örn. GRS80)"),
        ParamDef(key = "centralMeridian", label = "DOM / Orta Meridyen (°)", numeric = true, validator = numericValidator),
        ParamDef(key = "scaleFactor", label = "Scale Factor", numeric = true, validator = numericValidator),
        ParamDef(key = "falseEasting", label = "False Easting", numeric = true, validator = numericValidator),
        ParamDef(key = "falseNorthing", label = "False Northing", numeric = true, validator = numericValidator)
    )
    "ItrfParametersActivity" -> listOf(
        ParamDef(key = "frame", label = "Referans Çerçeve", validator = { if (it.isBlank()) "Boş bırakılamaz" else null }),
        ParamDef(key = "ellipsoid", label = "Elipsoid", validator = { if (it.isBlank()) "Boş bırakılamaz" else null }),
        ParamDef(key = "epoch", label = "Epoch Year", numeric = true, validator = { validateEpoch(it) })
    ) + sevenParamDefs
    "SevenParamsActivity" -> sevenParamDefs
    "FourParamsActivity" -> listOf(
        ParamDef(key = "dX", label = "dX (m)", numeric = true),
        ParamDef(key = "dY", label = "dY (m)", numeric = true),
        // Uyarı temizlendi (gereksiz let kaldırıldı)
        ParamDef(key = "rotation", label = "Rotation (deg)", numeric = true, validator = { v -> if (v.isBlank()) null else if (v.toDoubleOrNull()!=null) null else "Sayı" }),
        ParamDef(key = "scale", label = "Scale (ppm)", numeric = true)
    )
    "VerticalControlParametersActivity" -> listOf(
        ParamDef(key = "geoidSep", label = "Geoid Sep (m)", numeric = true),
        ParamDef(key = "orthoOffset", label = "Ortho Offset (m)", numeric = true),
        ParamDef(key = "dynamicCorr", label = "Dynamic Corr (Y/N)", validator = { if (it.isBlank() || it.uppercase() in listOf("Y","N")) null else "Y veya N" })
    )
    "VerticalAdjustmentParametersActivity" -> listOf(
        ParamDef(key = "refLevel", label = "Ref Level (m)", numeric = true),
        ParamDef(key = "shift", label = "Shift (m)", numeric = true),
        ParamDef(key = "scale", label = "Scale", numeric = true),
        ParamDef(key = "trend", label = "Trend", numeric = true)
    )
    "GridFileActivity" -> listOf(
        ParamDef(key = "gridPath", label = "Grid Path"),
        ParamDef(key = "checksum", label = "Checksum"),
        ParamDef(key = "version", label = "Version")
    )
    "GeoidFileActivity" -> listOf(
        ParamDef(key = "geoidPath", label = "Geoid Path"),
        ParamDef(key = "modelName", label = "Model Name"),
        ParamDef(key = "version", label = "Version")
    )
    "LocalOffsetsActivity" -> listOf(
        ParamDef(key = "dNorth", label = "dNorth (m)", numeric = true),
        ParamDef(key = "dEast", label = "dEast (m)", numeric = true),
        ParamDef(key = "dUp", label = "dUp (m)", numeric = true)
    )
    else -> listOf(
        ParamDef(key = "param1", label = "Param1"),
        ParamDef(key = "param2", label = "Param2")
    )
}

// Drop-down seçenekleri (eksik olan referanslar için eklendi)
private val frameOptions = listOf("ITRF96", "ITRF97", "ITRF2000", "ITRF2005", "ITRF2008", "ITRF2014")
private val ellipsoidOptions = listOf("GRS80", "WGS84", "Hayford", "International1924", "Clarke1866")

private fun validateEpoch(v: String): String? {
    if (v.isBlank()) return null // boş bırakılabilir
    val year = v.toIntOrNull() ?: return "Tam sayı yıl"
    if (year < 1980) return ">=1980 olmalı"
    if (year > 2100) return "2100 üstü?" // yumuşak uyarı
    return null
}

private fun validateRotation(v: String): String? {
    if (v.isBlank()) return null
    val d = v.toDoubleOrNull() ?: return "Sayı"
    if (kotlin.math.abs(d) > 30) return "Çok büyük (arcsec)" // pratik sınır
    return null
}

private fun arcsecToRad(value: String): String? = value.toDoubleOrNull()?.let { (it / 206264.806247).format(10) }

private fun Double.format(dec: Int): String = runCatching { "% .${dec}f".format(this).trim() }.getOrElse { toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenericParamScreen(activityId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dirty by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val defs = remember(activityId) { paramDefinitions(activityId) }

    data class FieldState(val def: ParamDef, val text: MutableState<String>, val error: MutableState<String?>)
    val fieldStates = remember(activityId) { defs.map { FieldState(it, mutableStateOf(""), mutableStateOf<String?>(null)) } }

    // --- Profil State'leri ---
    var profileName by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf("") }
    var showProfileDropdown by remember { mutableStateOf(false) }
    val profiles = remember { mutableStateMapOf<String, Map<String,String>>() } // profil adı -> param map

    // Profil apply
    fun applyProfile(name: String) {
        profiles[name]?.let { pMap ->
            fieldStates.forEach { fs ->
                pMap[fs.def.key]?.let { v -> fs.text.value = v; fs.error.value = fs.def.validator(v) }
            }
            dirty = true // Uygulandıktan sonra kaydetmek isteyebilir
        }
    }

    // Profil kaydet
    fun saveProfile() {
        val name = profileName.trim()
        if (name.isBlank()) return
        val map = fieldStates.associate { it.def.key to it.text.value.trim() }
        profiles[name] = map
        selectedProfile = name
        profileName = ""
        // DataStore persist
        scope.launch {
            context.coordParamsDataStore.edit { prefs ->
                // profil isim listesi
                val listKey = stringPreferencesKey("profiles_list:$activityId")
                val existing = (prefs[listKey] ?: "").split(',').filter { it.isNotBlank() }.toMutableSet()
                existing.add(name)
                prefs[listKey] = existing.joinToString(",")
                // her param ayrı saklanıyor
                map.forEach { (k, v) ->
                    prefs[stringPreferencesKey("profile:$activityId:$name:$k")] = v
                }
            }
            snackbarHostState.showSnackbar("Profil kaydedildi")
        }
    }

    // Profil sil
    fun deleteProfile(name: String) {
        if (!profiles.containsKey(name)) return
        profiles.remove(name)
        if (selectedProfile == name) selectedProfile = ""
        scope.launch {
            context.coordParamsDataStore.edit { prefs ->
                val listKey = stringPreferencesKey("profiles_list:$activityId")
                val remaining = (prefs[listKey] ?: "").split(',').filter { it.isNotBlank() && it != name }
                prefs[listKey] = remaining.joinToString(",")
                // param anahtarları kaldır
                defs.forEach { def ->
                    val pk = stringPreferencesKey("profile:$activityId:$name:${def.key}")
                    prefs.remove(pk)
                }
            }
            snackbarHostState.showSnackbar("Profil silindi")
        }
    }

    // İlk açılışta DataStore'dan yükle
    LaunchedEffect(activityId) {
        fieldStates.forEach { fs ->
            val key = activityId+":"+fs.def.key
            val loaded = loadParam(context, key)
            if (loaded.isNotEmpty() && fs.text.value.isEmpty()) fs.text.value = loaded
        }
        // Varsayılan başlangıç değerleri doldur (eğer boş kaldıysa)
        if (activityId == "ProjectionParametersActivity") {
            val defaults = mapOf(
                "frame" to "ITRF96",
                "ellipsoid" to "GRS80",
                "centralMeridian" to "30"
            )
            fieldStates.forEach { fs ->
                defaults[fs.def.key]?.let { defVal ->
                    if (fs.text.value.isEmpty()) fs.text.value = defVal
                }
            }
        } else if (activityId == "ItrfParametersActivity") {
            val defaults = mapOf(
                "frame" to "ITRF96",
                "ellipsoid" to "GRS80",
                "epoch" to "2025"
            )
            fieldStates.forEach { fs ->
                defaults[fs.def.key]?.let { defVal ->
                    if (fs.text.value.isEmpty()) fs.text.value = defVal
                }
            }
        }
        // Profilleri yükle
        val prefs = context.coordParamsDataStore.data.firstOrNullSafe()
        val listKey = stringPreferencesKey("profiles_list:$activityId")
        val listStr = prefs?.get(listKey) ?: ""
        val names = listStr.split(',').filter { it.isNotBlank() }
        names.forEach { name ->
            val paramMap = mutableMapOf<String,String>()
            defs.forEach { def ->
                val pk = stringPreferencesKey("profile:$activityId:$name:${def.key}")
                prefs?.get(pk)?.let { paramMap[def.key] = it }
            }
            if (paramMap.isNotEmpty()) profiles[name] = paramMap
        }
        if (selectedProfile.isBlank() && names.isNotEmpty()) selectedProfile = names.first()
    }

    fun saveAll() {
        scope.launch {
            fieldStates.forEach { fs ->
                val key = activityId+":"+fs.def.key
                saveParam(context, key, fs.text.value.trim())
            }
            dirty = false
            snackbarHostState.showSnackbar("Kaydedildi")
        }
    }

    val hasError = fieldStates.any { it.error.value != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(genericTitle(activityId)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    if (dirty && !hasError) {
                        IconButton(onClick = { saveAll() }) { Icon(Icons.Default.Save, contentDescription = "Kaydet") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Parametreleri girin / düzenleyin", style = MaterialTheme.typography.bodyMedium)
            fieldStates.forEach { fs ->
                val isDropdown = fs.def.key == "frame" || fs.def.key == "ellipsoid"
                if (isDropdown) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        val options: List<String> = if (fs.def.key == "frame") frameOptions else ellipsoidOptions
                        TextField(
                            value = fs.text.value,
                            onValueChange = { /* only selection via menu */ },
                            readOnly = true,
                            label = { Text(fs.def.label) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        fs.text.value = opt; expanded = false; dirty = true; fs.error.value = fs.def.validator(opt)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = fs.text.value,
                        onValueChange = {
                            fs.text.value = it; dirty = true; fs.error.value = fs.def.validator(it)
                        },
                        label = { Text(fs.def.label) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = fs.error.value != null,
                        supportingText = {
                            val err = fs.error.value
                            if (err != null) Text(err, color = MaterialTheme.colorScheme.error)
                            else if (fs.def.showDerived) {
                                val rad = arcsecToRad(fs.text.value)
                                if (rad != null) Text("Radyan: $rad", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (fs.def.numeric) KeyboardType.Number else KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {}),
                        trailingIcon = {
                            if (fs.error.value != null) Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            else if (fs.def.showDerived) Icon(Icons.Default.Calculate, null)
                        }
                    )
                }
            }
            // Profil Yönetimi Bölümü
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Text("Profiller", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profil Adı") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { saveProfile() }, enabled = profileName.isNotBlank()) { Text("Kaydet") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedProfile,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProfileDropdown = true },
                        readOnly = true,
                        label = { Text("Yüklü Profil") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = showProfileDropdown, onDismissRequest = { showProfileDropdown = false }) {
                        profiles.keys.sorted().forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                selectedProfile = name; showProfileDropdown = false; applyProfile(name)
                            })
                        }
                    }
                }
                OutlinedButton(onClick = { if (selectedProfile.isNotBlank()) applyProfile(selectedProfile) }, enabled = selectedProfile.isNotBlank()) { Text("Yükle") }
                OutlinedButton(onClick = { if (selectedProfile.isNotBlank()) deleteProfile(selectedProfile) }, enabled = selectedProfile.isNotBlank()) { Text("Sil") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { saveAll() }, enabled = dirty && !hasError, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(6.dp)); Text("Kaydet") }
                OutlinedButton(onClick = {
                    fieldStates.forEach { it.text.value = ""; it.error.value = null }; dirty = true
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(6.dp)); Text("Temizle") }
            }
            AssistChip(onClick = {}, label = { Text(
                when {
                    hasError -> "Hatalar var"
                    dirty -> "Değişiklikler kaydedilmedi"
                    else -> "Güncel"
                }
            ) }, leadingIcon = { Icon(
                when {
                    hasError -> Icons.Default.Warning
                    dirty -> Icons.Default.Edit
                    else -> Icons.Default.Check
                }, null) })
            Spacer(Modifier.height(32.dp))
            Text("Not: Parametreler DataStore ile kalıcı olarak saklanır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

abstract class BaseParamActivity: ComponentActivity() {
    abstract val id: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Tugis3Theme { GenericParamScreen(id) { finish() } } }
    }
}

@AndroidEntryPoint class ItrfParametersActivity: BaseParamActivity() { override val id = "ItrfParametersActivity" }
@AndroidEntryPoint class SevenParamsActivity: BaseParamActivity() { override val id = "SevenParamsActivity" }
@AndroidEntryPoint class FourParamsActivity: BaseParamActivity() { override val id = "FourParamsActivity" }
@AndroidEntryPoint class VerticalControlParametersActivity: BaseParamActivity() { override val id = "VerticalControlParametersActivity" }
@AndroidEntryPoint class VerticalAdjustmentParametersActivity: BaseParamActivity() { override val id = "VerticalAdjustmentParametersActivity" }
@AndroidEntryPoint class GridFileActivity: BaseParamActivity() { override val id = "GridFileActivity" }
@AndroidEntryPoint class GeoidFileActivity: BaseParamActivity() { override val id = "GeoidFileActivity" }
@AndroidEntryPoint class LocalOffsetsActivity: BaseParamActivity() { override val id = "LocalOffsetsActivity" }
