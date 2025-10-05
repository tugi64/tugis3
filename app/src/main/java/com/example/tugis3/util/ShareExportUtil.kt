package com.example.tugis3.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareExportUtil {
    private const val AUTH = "com.example.tugis3.fileprovider"

    private fun sanitize(base: String): String = base.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun writeTempFile(context: Context, baseName: String, ext: String, content: String, suffix: String? = null): File {
        val safe = sanitize(baseName.ifBlank { "export" }) + (suffix?.let { "_"+sanitize(it) } ?: "") + "_" + timestamp() + ext
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, safe)
        f.writeText(content)
        return f
    }

    fun shareText(context: Context, title: String, content: String, mime: String = "text/plain", suffix: String? = null) {
        val file = writeTempFile(
            context,
            title,
            when {
                mime.contains("kml") -> ".kml"
                mime.contains("geo") -> ".geojson"
                mime.contains("csv") -> ".csv"
                else -> ".txt"
            },
            content,
            suffix
        )
        val uri = FileProvider.getUriForFile(context, AUTH, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
