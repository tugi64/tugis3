package com.example.tugis3.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tugis3.model.GnssReceivingMode

@Composable
fun DeviceSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: GnssDeviceViewModel = hiltViewModel()
) {
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    val currentMode by viewModel.currentReceivingMode.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Başlık
                Text(
                    text = "Cihaz Ayarları",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Eğim Ölçüm Durumu
                SettingRow(
                    title = "Eğim Ölçüm",
                    value = if (deviceSettings.imuEnabled) "Aktif" else "Devre Dışı",
                    backgroundColor = if (deviceSettings.imuEnabled) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                )

                // Çözüm Durumu
                SettingRow(
                    title = "Çözüm Durumu",
                    value = deviceSettings.solutionStatus,
                    backgroundColor = Color(0xFFC8E6C9)
                )

                // HRMS Limit
                EditableSettingRow(
                    title = "HRMS Limit",
                    value = deviceSettings.hrmsLimit.toString(),
                    onValueChange = { newValue ->
                        newValue.toDoubleOrNull()?.let { viewModel.updateHrmsLimit(it) }
                    }
                )

                // VRMS Limit
                EditableSettingRow(
                    title = "VRMS Limit",
                    value = deviceSettings.vrmsLimit.toString(),
                    onValueChange = { newValue ->
                        newValue.toDoubleOrNull()?.let { viewModel.updateVrmsLimit(it) }
                    }
                )

                // PDOP Limit
                EditableSettingRow(
                    title = "PDOP Limit",
                    value = deviceSettings.pdopLimit.toString(),
                    onValueChange = { newValue ->
                        newValue.toDoubleOrNull()?.let { viewModel.updatePdopLimit(it) }
                    }
                )

                // Geçkme Limiti
                SettingRow(
                    title = "Geçkme Limiti",
                    value = deviceSettings.passLimit.toString()
                )

                // Aynı İşlem Kullanımı
                SwitchSettingRow(
                    title = "Aynı İşlem Kullanımı",
                    checked = deviceSettings.useSameOperation,
                    onCheckedChange = { viewModel.toggleSameOperation() }
                )

                // Girilen Son Noktaya Odaklan
                SwitchSettingRow(
                    title = "Girilen Son Noktaya Odaklan",
                    checked = deviceSettings.focusLastPoint,
                    onCheckedChange = { viewModel.toggleFocusLastPoint() }
                )

                // Nokta Adı
                EditableSettingRow(
                    title = "Nokta Adı",
                    value = deviceSettings.pointName,
                    onValueChange = { viewModel.updatePointName(it) }
                )

                // Nokta Adı Artışı
                EditableSettingRow(
                    title = "Nokta Adı Artışı",
                    value = deviceSettings.pointIncrement.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { viewModel.updatePointIncrement(it) }
                    }
                )

                // Varsayılan Kod
                SettingRow(
                    title = "Varsayılan Kod",
                    value = deviceSettings.defaultCode
                )

                // Oturum Süresi
                EditableSettingRow(
                    title = "Oturum Süresi",
                    value = deviceSettings.sessionDuration.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { viewModel.updateSessionDuration(it) }
                    }
                )

                // Epok Sayısı
                EditableSettingRow(
                    title = "Epok Sayısı",
                    value = deviceSettings.epochCount.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { viewModel.updateEpochCount(it) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Alım Modları
                Text(
                    text = "Alım Modları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                GnssReceivingMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { viewModel.updateReceivingMode(mode) }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Varsayılan Konfigürasyonlar Button
                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Varsayılan Konfigürasyonlar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tamam/İptal Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Tamam")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    backgroundColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .then(
                    if (backgroundColor != null) {
                        Modifier
                            .background(
                                backgroundColor,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    } else {
                        Modifier
                    }
                ),
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableSettingRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )

        if (isEditing) {
            OutlinedTextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier.width(100.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                singleLine = true
            )
            IconButton(
                onClick = {
                    onValueChange(editValue)
                    isEditing = false
                }
            ) {
                Text("✓", fontSize = 16.sp)
            }
        } else {
            TextButton(
                onClick = { isEditing = true }
            ) {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
