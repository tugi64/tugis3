package com.example.tugis3.settings

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATASTORE_NAME = "app_settings"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object AppSettings {
    private val KEY_SIMPLIFY_SCALE = doublePreferencesKey("simplify_scale")
    private const val DEFAULT_SCALE = 1.0

    fun simplifyScaleFlow(context: Context): Flow<Double> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_SIMPLIFY_SCALE] ?: DEFAULT_SCALE }

    suspend fun setSimplifyScale(context: Context, scale: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SIMPLIFY_SCALE] = scale.coerceIn(0.25, 3.0)
        }
    }
}

