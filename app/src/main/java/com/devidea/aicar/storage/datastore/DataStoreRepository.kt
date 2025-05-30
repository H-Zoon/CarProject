package com.devidea.aicar.storage.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devidea.aicar.module.AppModule
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.ui.main.components.gaugeItems
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val SAVE_DEVICE_NAME = stringPreferencesKey("device_name")
    private val SAVE_DEVICE_ADDR = stringPreferencesKey("device_addr")
    private val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect_on_charge")

    private val CONNECT_DATE_KEY = stringPreferencesKey("connect_date")
    private val RECENT_MILEAGE_KEY = intPreferencesKey("recent_mileage")
    private val AUTO_DRIVING_RECORD_ENABLED = booleanPreferencesKey("driving_record_enabled")
    private val MANUAL_DRIVING_RECORD_ENABLED = booleanPreferencesKey("driving_record_manual_enabled")

    private val GAUGE_ORDER_KEY = stringPreferencesKey("gauge_order")

    private val gaugeIdPool = gaugeItems.map { it.id }.toSet()  // 허용 ID

    private val mutex = Mutex()

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
    /** 저장된 디바이스를 Flow로 제공 */
    val getDevice: Flow<ScannedDevice?> = dataStore.data
        .map { prefs ->
            val name = prefs[SAVE_DEVICE_NAME]
            val addr = prefs[SAVE_DEVICE_ADDR]
            if (name != null && addr != null) ScannedDevice(name, addr) else null
        }

    /** 디바이스 저장 */
    suspend fun saveDevice(device: ScannedDevice) {
        dataStore.edit { prefs ->
            prefs[SAVE_DEVICE_NAME] = device.name.orEmpty()
            prefs[SAVE_DEVICE_ADDR] = device.address
        }
    }

    // 저장
    suspend fun setAutoConnectOnCharge(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_CONNECT_KEY] = enabled
        }
    }

    // Flow
    val isAutoConnectOnCharge: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_CONNECT_KEY] ?: false }

    /* ---------- Connect Date ---------- */
    suspend fun saveConnectData(date: String) {
        dataStore.edit { preferences ->
            preferences[CONNECT_DATE_KEY] = date
        }
    }

    fun getConnectDate(): Flow<String> {
        return dataStore.data
            .map { preferences ->
                preferences[CONNECT_DATE_KEY] ?: ""
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

    suspend fun setDrivingRecode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_DRIVING_RECORD_ENABLED] = value
        }
    }

    fun getDrivingRecodeSetDate(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                preferences[AUTO_DRIVING_RECORD_ENABLED] == true
            }
    }
}