package com.example.tugis3.core.cad.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.tugis3.core.cad.model.CadEntity
import com.example.tugis3.core.cad.parse.DxfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

interface CadRepository {
    suspend fun loadDxf(contentResolver: ContentResolver, uri: Uri): List<CadEntity>
    suspend fun parseStream(stream: InputStream): List<CadEntity>
}

class CadRepositoryImpl @javax.inject.Inject constructor(private val dxfParser: DxfParser) : CadRepository {
    override suspend fun loadDxf(contentResolver: ContentResolver, uri: Uri): List<CadEntity> =
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { dxfParser.parse(it) } ?: emptyList()
        }

    override suspend fun parseStream(stream: InputStream): List<CadEntity> =
        withContext(Dispatchers.IO) { dxfParser.parse(stream) }
}
