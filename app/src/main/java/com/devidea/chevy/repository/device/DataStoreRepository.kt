package com.devidea.chevy.repository.device

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val FIRST_LUNCH_KEY = booleanPreferencesKey("first_lunch")
    private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")
    private val MAX_HISTORY_SIZE = 10 // 최대 히스토리 수

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

    suspend fun saveFirstLunch(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[FIRST_LUNCH_KEY] = value
        }
    }

    fun getFirstLunch(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                preferences[FIRST_LUNCH_KEY] ?: true
            }
    }

    /**
     * 검색어를 히스토리에 추가합니다.
     * 중복된 검색어는 제거되고, 최신 검색어가 맨 앞에 위치합니다.
     * 최대 히스토리 수를 초과하면 가장 오래된 검색어가 제거됩니다.
     */
    suspend fun addSearchQuery(query: String) {
        dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
            // 중복 제거
            currentHistory.remove(query)
            // 최신 검색어 추가
            currentHistory.add(0, query)
            // 최대 히스토리 수 유지
            /*if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.removeAt(currentHistory.size - 1)
            }*/
            // 쉼표로 구분된 문자열로 저장
            preferences[SEARCH_HISTORY_KEY] = currentHistory.joinToString(",")
        }
    }

    /**
     * 저장된 검색 히스토리를 Flow<List<String>> 형태로 반환합니다.
     */
    fun getSearchHistory(): Flow<List<String>> {
        return dataStore.data
            .map { preferences ->
                preferences[SEARCH_HISTORY_KEY]
                    ?.split(",")
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
            }
    }

    /**
     * 검색 히스토리를 전체 삭제합니다.
     */
    suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY_KEY)
        }
    }

    /**
     * 특정 검색어를 히스토리에서 제거합니다.
     */
    suspend fun removeSearchQuery(query: String) {
        dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
            currentHistory.remove(query)
            preferences[SEARCH_HISTORY_KEY] = currentHistory.joinToString(",")
        }
    }
}