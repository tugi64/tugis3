package com.example.tugis3.ui.map

import android.app.Activity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tugis3.ui.theme.Tugis3Theme

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Read potential lat/lon from intent extras
        val lat = intent?.getDoubleExtra("lat", Double.NaN)
        val lon = intent?.getDoubleExtra("lon", Double.NaN)
        setContent {
            Tugis3Theme {
                MapScreen(initialLat = if (lat != null && !lat.isNaN()) lat else null,
                    initialLon = if (lon != null && !lon.isNaN()) lon else null)
            }
        }
    }
}

@Composable
fun MapScreen(initialLat: Double? = null, initialLon: Double? = null) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Text(
            text = "Map View",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // WebView inside Compose
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // If initial coords provided, add marker via JS
                        try {
                            if (initialLat != null && initialLon != null) {
                                val js = "addMarker(${initialLat}, ${initialLon}, 'Selected Point');"
                                view?.evaluateJavascript(js, null)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
                loadUrl("file:///android_asset/map.html")
            }
        }, update = { view ->
            // no-op for now
        }, modifier = Modifier.weight(1f))
    }
}
