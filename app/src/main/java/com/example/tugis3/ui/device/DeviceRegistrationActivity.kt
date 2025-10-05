package com.example.tugis3.ui.device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tugis3.ui.theme.Tugis3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tugis3Theme {
                DeviceRegistrationScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRegistrationScreen(onBackPressed: () -> Unit) {
    var deviceSerial by remember { mutableStateOf("E600351920028") }
    var activationCode by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var licenseType by remember { mutableStateOf("Professional") }
    var isRegistered by remember { mutableStateOf(true) }
    var remainingDays by remember { mutableStateOf(365) }

    val licenseTypes = listOf("Basic", "Professional", "Enterprise", "Unlimited")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cihaz Kaydı") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Lisans Durumu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRegistered && remainingDays > 30)
                            MaterialTheme.colorScheme.primaryContainer
                        else if (isRegistered && remainingDays > 0)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (isRegistered) "Lisans Aktif" else "Lisans Yok",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isRegistered) {
                                    Text("$remainingDays gün kaldı")
                                }
                            }

                            Icon(
                                if (isRegistered) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isRegistered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lisans Tipi:")
                            Text(licenseType, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cihaz Serisi:")
                            Text(deviceSerial, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                // Cihaz Bilgileri
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cihaz Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = deviceSerial,
                            onValueChange = { deviceSerial = it },
                            label = { Text("Seri Numarası") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Memory, null) },
                            enabled = !isRegistered
                        )

                        Spacer(Modifier.height(8.dp))

                        var licenseExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = licenseExpanded,
                            onExpandedChange = { licenseExpanded = !licenseExpanded && !isRegistered }
                        ) {
                            OutlinedTextField(
                                value = licenseType,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Lisans Tipi") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = licenseExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                enabled = !isRegistered
                            )
                            ExposedDropdownMenu(
                                expanded = licenseExpanded,
                                onDismissRequest = { licenseExpanded = false }
                            ) {
                                licenseTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            licenseType = type
                                            licenseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Aktivasyon Kodu
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aktivasyon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = activationCode,
                            onValueChange = { activationCode = it },
                            label = { Text("Aktivasyon Kodu") },
                            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Key, null) },
                            enabled = !isRegistered
                        )

                        Spacer(Modifier.height(12.dp))

                        if (!isRegistered) {
                            Button(
                                onClick = {
                                    if (activationCode.isNotBlank()) {
                                        isRegistered = true
                                        remainingDays = 365
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = activationCode.isNotBlank()
                            ) {
                                Icon(Icons.Default.Verified, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Aktivasyon Kodunu Doğrula")
                            }
                        } else {
                            Text(
                                "✅ Cihaz başarıyla aktif edilmiştir",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                // Kullanıcı Bilgileri
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kullanıcı Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("E-posta Adresi") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null) }
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Şirket/Kurum Adı") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Business, null) }
                        )
                    }
                }
            }

            item {
                // Lisans Özellikleri
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Lisans Özellikleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        val features = when (licenseType) {
                            "Basic" -> listOf(
                                "Nokta Ölçümü" to true,
                                "RTK Desteği" to true,
                                "NTRIP Bağlantısı" to true,
                                "Detay Alımı" to false,
                                "CAD Entegrasyonu" to false,
                                "Aplikasyon" to false,
                                "Fotogrametri" to false,
                                "Yol Tasarımı" to false
                            )
                            "Professional" -> listOf(
                                "Nokta Ölçümü" to true,
                                "RTK Desteği" to true,
                                "NTRIP Bağlantısı" to true,
                                "Detay Alımı" to true,
                                "CAD Entegrasyonu" to true,
                                "Aplikasyon" to true,
                                "Fotogrametri" to false,
                                "Yol Tasarımı" to false
                            )
                            "Enterprise" -> listOf(
                                "Nokta Ölçümü" to true,
                                "RTK Desteği" to true,
                                "NTRIP Bağlantısı" to true,
                                "Detay Alımı" to true,
                                "CAD Entegrasyonu" to true,
                                "Aplikasyon" to true,
                                "Fotogrametri" to true,
                                "Yol Tasarımı" to true
                            )
                            else -> listOf(
                                "Tüm Özellikler" to true,
                                "Sınırsız Kullanım" to true,
                                "Teknik Destek" to true,
                                "Özel Geliştirme" to true
                            )
                        }

                        features.forEach { (feature, enabled) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    feature,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Support ve İletişim
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Destek ve İletişim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Online support */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Support, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Online Destek")
                            }

                            OutlinedButton(
                                onClick = { /* Contact dealer */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContactSupport, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Bayi İletişim")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Teknik destek için: support@tugis.com\nSatış bilgisi için: sales@tugis.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isRegistered) {
                item {
                    // Lisans Yenileme/Transfer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Lisans Yönetimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { /* Renew license */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Autorenew, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lisans Yenile")
                                }

                                OutlinedButton(
                                    onClick = {
                                        isRegistered = false
                                        activationCode = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.DeleteForever, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lisansı Sıfırla")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
