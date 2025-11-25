package com.example.tugis3.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Tekil DataStore: tüm uygulama "coord_params" altında paylaşır
val Context.coordParamsDataStore by preferencesDataStore(name = "coord_params")

object CoordPrefs {
    enum class CoordOrder { X_FIRST, Y_FIRST }
    enum class AxisMap { EASTING_TO_Y_NORTHING_TO_X, EASTING_TO_X_NORTHING_TO_Y }

    private val KEY_ORDER = stringPreferencesKey("coord_order")
    private val KEY_AXIS_MAP = stringPreferencesKey("coord_axis_map")

    fun orderFlow(context: Context): Flow<CoordOrder> = context.coordParamsDataStore.data
        .map { prefs: Preferences ->
            when (prefs[KEY_ORDER]) {
                "X_FIRST" -> CoordOrder.X_FIRST
                "Y_FIRST" -> CoordOrder.Y_FIRST
                else -> CoordOrder.Y_FIRST // varsayılan: kullanıcı talebine uygun "Önce Y"
            }
        }

    fun axisMapFlow(context: Context): Flow<AxisMap> = context.coordParamsDataStore.data
        .map { prefs: Preferences ->
            when (prefs[KEY_AXIS_MAP]) {
                AxisMap.EASTING_TO_X_NORTHING_TO_Y.name -> AxisMap.EASTING_TO_X_NORTHING_TO_Y
                AxisMap.EASTING_TO_Y_NORTHING_TO_X.name -> AxisMap.EASTING_TO_Y_NORTHING_TO_X
                else -> AxisMap.EASTING_TO_Y_NORTHING_TO_X // varsayılan: E->Y, N->X
            }
        }

    suspend fun setOrder(context: Context, order: CoordOrder) {
        context.coordParamsDataStore.edit { it[KEY_ORDER] = order.name }
    }

    suspend fun setAxisMap(context: Context, map: AxisMap) {
        context.coordParamsDataStore.edit { it[KEY_AXIS_MAP] = map.name }
    }

    // Java tarafında senkron kullanım için yardımcılar
    @JvmStatic
    fun axisMapNow(context: Context): AxisMap = runBlocking { axisMapFlow(context).first() }

    @JvmStatic
    fun orderNow(context: Context): CoordOrder = runBlocking { orderFlow(context).first() }
}
