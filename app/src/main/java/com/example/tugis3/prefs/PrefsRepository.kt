package com.example.tugis3.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("tugis_prefs")

@Suppress("unused")
@Singleton
class PrefsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val COGO_TAB = intPreferencesKey("cogo_tab")
        val STAKE_H_TOL = doublePreferencesKey("stake_h_tol")
        val STAKE_V_TOL = doublePreferencesKey("stake_v_tol")
        val LINE_INTERVAL = doublePreferencesKey("line_interval")
        val LINE_LAT_TOL = doublePreferencesKey("line_lat_tol")
        val LINE_CHAIN_TOL = doublePreferencesKey("line_chain_tol")
        // val LAST_DEVICE_ID = stringPreferencesKey("last_device_id") // ileride cihaz seçimi için
        val STAKEOUT_PT_NAME = stringPreferencesKey("stakeout_pt_name")
        val LINE_NAME = stringPreferencesKey("line_name")
        val AUTO_PROJ = booleanPreferencesKey("auto_projection_enabled")
        val FAV_PROJECTIONS = stringPreferencesKey("fav_proj_defs") // ';' ile ayrılmış anahtar listesi
    }

    val cogoTab: Flow<Int> = context.dataStore.data.map { it[Keys.COGO_TAB] ?: 0 }
    suspend fun setCogoTab(i:Int) = context.dataStore.edit { it[Keys.COGO_TAB] = i }

    val stakeHorizontalTol: Flow<Double> = context.dataStore.data.map { it[Keys.STAKE_H_TOL] ?: 0.10 }
    val stakeVerticalTol: Flow<Double> = context.dataStore.data.map { it[Keys.STAKE_V_TOL] ?: 0.05 }
    suspend fun setStakeHorizontalTol(v:Double) = context.dataStore.edit { it[Keys.STAKE_H_TOL] = v }
    suspend fun setStakeVerticalTol(v:Double) = context.dataStore.edit { it[Keys.STAKE_V_TOL] = v }

    val lineInterval: Flow<Double> = context.dataStore.data.map { it[Keys.LINE_INTERVAL] ?: 10.0 }
    val lineLatTol: Flow<Double> = context.dataStore.data.map { it[Keys.LINE_LAT_TOL] ?: 0.20 }
    val lineChainTol: Flow<Double> = context.dataStore.data.map { it[Keys.LINE_CHAIN_TOL] ?: 0.50 }
    suspend fun setLineInterval(v:Double) = context.dataStore.edit { it[Keys.LINE_INTERVAL] = v }
    suspend fun setLineLatTol(v:Double) = context.dataStore.edit { it[Keys.LINE_LAT_TOL] = v }
    suspend fun setLineChainTol(v:Double) = context.dataStore.edit { it[Keys.LINE_CHAIN_TOL] = v }

    // Cihaz id fonksiyonları kaldırıldı / ileride entegre edilecek
    // val lastDeviceId: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_DEVICE_ID] }
    // suspend fun setLastDeviceId(id:String?) = context.dataStore.edit { if (id==null) it.remove(Keys.LAST_DEVICE_ID) else it[Keys.LAST_DEVICE_ID]=id }

    val stakeoutPointName: Flow<String?> = context.dataStore.data.map { it[Keys.STAKEOUT_PT_NAME] }
    suspend fun setStakeoutPointName(name:String?) = context.dataStore.edit { if (name==null) it.remove(Keys.STAKEOUT_PT_NAME) else it[Keys.STAKEOUT_PT_NAME]=name }

    val lineNameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.LINE_NAME] }
    suspend fun setLineName(name:String) = context.dataStore.edit { it[Keys.LINE_NAME]=name }

    // Otomatik projeksiyon (varsayılan true)
    val autoProjectionEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_PROJ] ?: true }
    suspend fun setAutoProjectionEnabled(v:Boolean) = context.dataStore.edit { it[Keys.AUTO_PROJ] = v }

    // --- Favori projeksiyonlar ---
    val favoriteProjectionKeys: Flow<Set<String>> = context.dataStore.data.map { pref ->
        pref[Keys.FAV_PROJECTIONS]
            ?.split(';')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }
    private suspend fun persistFavoriteSet(set:Set<String>) = context.dataStore.edit { prefs ->
        if (set.isEmpty()) prefs.remove(Keys.FAV_PROJECTIONS) else prefs[Keys.FAV_PROJECTIONS] = set.joinToString(";")
    }
    suspend fun addFavorite(key:String) = context.dataStore.edit { prefs ->
        val cur = prefs[Keys.FAV_PROJECTIONS]?.split(';')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
        if (cur.add(key)) prefs[Keys.FAV_PROJECTIONS] = cur.joinToString(";")
    }
    suspend fun removeFavorite(key:String) = context.dataStore.edit { prefs ->
        val cur = prefs[Keys.FAV_PROJECTIONS]?.split(';')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
        if (cur.remove(key)) {
            if (cur.isEmpty()) prefs.remove(Keys.FAV_PROJECTIONS) else prefs[Keys.FAV_PROJECTIONS] = cur.joinToString(";")
        }
    }
    suspend fun toggleFavorite(key:String) = context.dataStore.edit { prefs ->
        val cur = prefs[Keys.FAV_PROJECTIONS]?.split(';')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
        if (!cur.add(key)) cur.remove(key)
        if (cur.isEmpty()) prefs.remove(Keys.FAV_PROJECTIONS) else prefs[Keys.FAV_PROJECTIONS] = cur.joinToString(";")
    }
}
