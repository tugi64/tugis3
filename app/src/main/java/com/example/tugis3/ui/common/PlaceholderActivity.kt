package com.example.tugis3.ui.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.tugis3.ui.theme.Tugis3Theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember

class PlaceholderActivity : ComponentActivity() {
    companion object { const val EXTRA_TITLE = "title" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Placeholder"
        setContent { Tugis3Theme { PlaceholderScreen(title, onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    val help = remember(title) { HelpContentProvider.get(title) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { pad ->
        if (help == null) {
            Text(
                "Bu ekran henüz tamamlanmadı.",
                modifier = Modifier.padding(pad).padding(16.dp)
            )
        } else {
            HelpContentView(help = help, modifier = Modifier.padding(pad))
        }
    }
}

@Composable
private fun HelpContentView(help: HelpContentProvider.HelpContent, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(help.title, style = MaterialTheme.typography.titleLarge)
        Text(help.intro, style = MaterialTheme.typography.bodyMedium)

        if (help.steps.isNotEmpty()) {
            SectionHeader("Adımlar")
            help.steps.forEachIndexed { i, s ->
                BulletLine(index = i + 1, text = s)
            }
        }
        if (help.tips.isNotEmpty()) {
            SectionHeader("İpuçları")
            help.tips.forEach { s -> BulletLine(icon = "💡", text = s) }
        }
        if (help.warnings.isNotEmpty()) {
            SectionHeader("Uyarılar")
            help.warnings.forEach { s ->
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(s, modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        help.devNote?.let {
            SectionHeader("Geliştirici Notu")
            AssistChip(onClick = {}, label = { Text(it) })
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Bu içerik kullanım kılavuzlarının özgün ve özetlenmiş bir yorumudur.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BulletLine(index: Int? = null, icon: String? = null, text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val prefix = when {
            icon != null -> icon
            index != null -> "$index."
            else -> "•"
        }
        Text(prefix, modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
