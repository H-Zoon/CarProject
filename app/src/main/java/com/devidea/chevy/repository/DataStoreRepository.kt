package com.devidea.chevy.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class DataStoreRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val CONNECT_DATE_KEY = longPreferencesKey("connect_date")
    private val RECENT_MILEAGE_KEY = intPreferencesKey("recent_mileage")
    private val RECENT_FUEL_EFFICIENCY_KEY = floatPreferencesKey("recent_fuel")

    suspend fun saveConnectData() {
        dataStore.edit { preferences ->
            preferences[CONNECT_DATE_KEY] = LocalDate.now().toEpochDay()
        }
    }

    fun getConnectDate(): Flow<Long> {
        return dataStore.data
            .map { preferences ->
                val savedDate = preferences[CONNECT_DATE_KEY]?.let { LocalDate.ofEpochDay(it) }
                savedDate?.let {
                    ChronoUnit.DAYS.between(it, LocalDate.now())
                } ?: -1
            }
    }

    suspend fun saveMileageData(value: Int) {
        dataStore.edit { preferences ->
            preferences[RECENT_MILEAGE_KEY] = value
        }
    }

    fun getMileageDate(): Flow<Int> {
        return dataStore.data
            .map { preferences ->
                preferences[RECENT_MILEAGE_KEY] ?: -1
            }
    }

    suspend fun saveFuelData(value: Float) {
        dataStore.edit { preferences ->
            preferences[RECENT_FUEL_EFFICIENCY_KEY] = value
        }
    }

    fun getFuelDate(): Flow<Float> {
        return dataStore.data
            .map { preferences ->
                preferences[RECENT_FUEL_EFFICIENCY_KEY] ?: -1f
            }
    }
}