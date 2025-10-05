package com.example.tugis3.ui.navigation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gelişmiş menü durum yönetimi:
 * - Favoriler (uzun basarak ekle/çıkar)
 * - Son kullanılanlar (otomatik kayıt, en fazla 10)
 * - Temizleme / tekil silme işlemleri
 * Kalıcılık DataStore üzerinden sağlanır (ayrı bir dosya: menu_prefs.preferences_pb)
 */
private val Context.menuDataStore by preferencesDataStore("menu_prefs")

@HiltViewModel
class MenuStateViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("menu_favorites")
        val RECENTS = stringPreferencesKey("menu_recents") // virgül ile ayrılmış id listesi
    }

    // DataStore kaynaklı akışlar
    val favorites: StateFlow<Set<String>> = appContext.menuDataStore.data
        .map { it[Keys.FAVORITES] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val recents: StateFlow<List<String>> = appContext.menuDataStore.data
        .map { prefs ->
            val raw = prefs[Keys.RECENTS].orEmpty()
            raw.split(',').mapNotNull { it.trim().ifBlank { null } }.take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                appContext.menuDataStore.edit { prefs ->
                    val current = prefs[Keys.FAVORITES] ?: emptySet()
                    prefs[Keys.FAVORITES] = if (current.contains(id)) current - id else current + id
                }
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun recordUsage(id: String) {
        viewModelScope.launch {
            appContext.menuDataStore.edit { prefs ->
                val raw = prefs[Keys.RECENTS].orEmpty()
                val current = raw.split(',').mapNotNull { it.ifBlank { null } }.toMutableList()
                current.remove(id) // duplicate önle
                current.add(0, id)
                prefs[Keys.RECENTS] = current.take(10).joinToString(",")
            }
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            appContext.menuDataStore.edit { it[Keys.RECENTS] = "" }
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            appContext.menuDataStore.edit { it[Keys.FAVORITES] = emptySet() }
        }
    }

    fun removeRecent(id: String) {
        viewModelScope.launch {
            appContext.menuDataStore.edit { prefs ->
                val raw = prefs[Keys.RECENTS].orEmpty()
                val current = raw.split(',').mapNotNull { it.ifBlank { null } }.toMutableList()
                current.remove(id)
                prefs[Keys.RECENTS] = current.joinToString(",")
            }
        }
    }
}
