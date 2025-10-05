package com.example.tugis3.core.cad

import android.content.ContentResolver
import android.net.Uri
import com.example.tugis3.core.cad.model.CadEntity
import com.example.tugis3.core.cad.parse.DxfParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CadService @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val parser = DxfParser()

    private val _entities = MutableStateFlow<List<CadEntity>>(emptyList())
    val entities: StateFlow<List<CadEntity>> = _entities.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun clear() {
        _entities.value = emptyList(); _status.value = null
    }

    fun loadFromUri(resolver: ContentResolver, uri: Uri) {
        scope.launch {
            runCatching {
                resolver.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalArgumentException("Boş giriş akışı")
                    val name = uri.lastPathSegment?.lowercase() ?: ""
                    if (name.endsWith(".dwg")) {
                        _status.value = "DWG desteği henüz yok. DXF dönüştürüp tekrar deneyin."
                        return@use
                    }
                    if (!name.endsWith(".dxf")) {
                        _status.value = "Desteklenmeyen format (yalnızca .dxf / .dwg)."
                        return@use
                    }
                    val list = parser.parse(input)
                    _entities.value = list
                    _status.value = "Yüklendi: ${list.size} varlık"
                }
            }.onFailure {
                _status.value = "Hata: ${it.message}"; _entities.value = emptyList()
            }
        }
    }
}
