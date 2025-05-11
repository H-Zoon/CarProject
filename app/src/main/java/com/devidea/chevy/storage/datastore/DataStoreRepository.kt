package com.devidea.chevy.storage.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devidea.chevy.module.AppModule
import com.devidea.chevy.ui.main.compose.gauge.gaugeItems
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @AppModule.ApplicationScope private val appScope: CoroutineScope
) {
    init {
        appScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                if (!prefs.contains(GAUGE_ORDER_KEY)) {
                    prefs[GAUGE_ORDER_KEY] = gaugeIdPool.joinToString(",")
                }
            }
        }
    }

    private val TAG = "GaugeRepository"
    private val CONNECT_DATE_KEY = longPreferencesKey("connect_date")
    private val RECENT_MILEAGE_KEY = intPreferencesKey("recent_mileage")
    private val RECENT_FUEL_EFFICIENCY_KEY = floatPreferencesKey("recent_fuel")

    private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")
    private val GAUGE_ORDER_KEY = stringPreferencesKey("gauge_order")
    private val MAX_HISTORY_SIZE = 10 // 최대 히스토리 수

    /* ---------- Gauge 선택 저장 / 조회 ---------- */
    /*    private val MIN_GAUGES = 1          // 최소 1개 이상
        private val MAX_GAUGES = 8          // 최대 8개까지*/
    private val gaugeIdPool = gaugeItems.map { it.id }.toSet()  // 허용 ID

    private val mutex = Mutex()               // ⚠️ 동시 접근 보호

    /** 선택된 Gauge ID 목록을 Flow 로 노출 (기본값은 [defaultGaugeIds]) */
    val selectedGaugeIds: Flow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[GAUGE_ORDER_KEY]
                ?.split(",")
                ?.filter { it in gaugeIdPool }
                ?: emptyList()
        }

    suspend fun toggleGauge(id: String) {
        if (id !in gaugeIdPool) {
            Log.d(TAG, "toggleGauge: invalid id='$id', not in gaugeIdPool")
            return
        }
        Log.d(TAG, "toggleGauge: START id='$id'")

        mutex.withLock {
            dataStore.edit { prefs ->
                val raw = prefs[GAUGE_ORDER_KEY]
                Log.d(TAG, "toggleGauge: prefs before edit raw='$raw'")

                val cur = raw
                    ?.split(",")
                    ?.filter { it in gaugeIdPool }
                    ?.toMutableList()
                    ?: mutableListOf()
                Log.d(TAG, "toggleGauge: current list before change=$cur")

                if (id in cur) {
                    cur.remove(id)
                    Log.d(TAG, "toggleGauge: removed '$id', new list=$cur")

                } else {
                    cur.add(id)
                    Log.d(TAG, "toggleGauge: added '$id', new list=$cur")
                }
                val joined = cur.joinToString(",")
                prefs[GAUGE_ORDER_KEY] = joined
                Log.d(TAG, "toggleGauge: prefs after edit joined='$joined'")
            }
        }

        Log.d(TAG, "toggleGauge: END id='$id'")
    }

    /* ---------- index 기반 스왑 ---------- */
    suspend fun swapGauge(from: Int, to: Int) {
        if (from == to) return
        mutex.withLock {
            dataStore.edit { prefs ->
                val cur = prefs[GAUGE_ORDER_KEY]
                    ?.split(",")
                    ?.filter { it in gaugeIdPool }
                    ?.toMutableList()
                    ?: return@edit

                if (from !in cur.indices || to !in cur.indices) return@edit
                cur.apply { Collections.swap(this, from, to) }
                prefs[GAUGE_ORDER_KEY] = cur.joinToString(",")
            }
        }
    }

    /* ---------- 초기화(전체 선택) ---------- */
    suspend fun resetAllGauges() = mutex.withLock {
        dataStore.edit { prefs ->
            prefs[GAUGE_ORDER_KEY] = gaugeIdPool.joinToString(",")
        }
    }

    /* ---------- Connect Date ---------- */
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

    /* ---------- Mileage ---------- */
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

    /* ---------- Fuel Efficiency ---------- */
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

    /**
     * 검색어를 히스토리에 추가합니다.
     * 중복된 검색어는 제거되고, 최신 검색어가 맨 앞에 위치합니다.
     * 최대 히스토리 수를 초과하면 가장 오래된 검색어가 제거됩니다.
     */
    suspend fun addSearchQuery(query: String) {
        dataStore.edit { preferences ->
            val currentHistory =
                preferences[SEARCH_HISTORY_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
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
            val currentHistory =
                preferences[SEARCH_HISTORY_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
            currentHistory.remove(query)
            preferences[SEARCH_HISTORY_KEY] = currentHistory.joinToString(",")
        }
    }
}