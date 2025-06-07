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
    /**
     * 초기화 블록: 애플리케이션 스코프에서 실행하여,
     * GAUGE_ORDER_KEY가 아직 존재하지 않으면 기본값으로 gaugeIdPool 전체를 쉼표(,)로 이어 붙여 저장
     */
    init {
        appScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                // 만약 설정에 GAUGE_ORDER_KEY가 없으면 초기값으로 설정
                if (!prefs.contains(GAUGE_ORDER_KEY)) {
                    prefs[GAUGE_ORDER_KEY] = gaugeIdPool.joinToString(",")
                }
            }
        }
    }

    private val TAG = "GaugeRepository"

    // Device 이름/주소를 저장할 키
    private val SAVE_DEVICE_NAME = stringPreferencesKey("device_name")
    private val SAVE_DEVICE_ADDR = stringPreferencesKey("device_addr")

    // 충전 시 자동 연결 여부를 저장할 키
    private val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect_on_charge")

    // 마지막 연결 날짜를 저장할 키 (문자열 형식, 예: "2023-06-03")
    private val CONNECT_DATE_KEY = stringPreferencesKey("connect_date")

    // 자동 주행 기록 여부를 저장할 키 (Boolean)
    private val AUTO_DRIVING_RECORD_ENABLED = booleanPreferencesKey("driving_record_enabled")

    // 계산할 유류비 저장을 위한 키
    private val FUEL_COST_KEY = intPreferencesKey("fuel_cost")

    // 계기판 순서를 저장할 키 (문자열로 "id1,id2,..." 형식)
    private val GAUGE_ORDER_KEY = stringPreferencesKey("gauge_order")

    // 허용 가능한 게이지 ID 모음. 외부에서 사용되는 gaugeItems 리스트를 id만 모아서 Set으로 만듦.
    private val gaugeIdPool = gaugeItems.map { it.id }.toSet()

    private val DTC_DB_INITIALIZED_KEY = booleanPreferencesKey("dtc_db_initialized")

    // 동시성 제어를 위한 뮤텍스. 여러 코루틴에서 동시에 데이터 편집 시 충돌을 막기 위함
    private val mutex = Mutex()

    /**
     * 선택된 게이지 ID 목록을 Flow로 노출
     *
     * - dataStore.data는 Preferences 객체의 Flow를 제공
     * - GAUGE_ORDER_KEY에 저장된 문자열("id1,id2,...")을 쉼표로 분리해서 List<String>으로 반환
     * - 키가 없거나 필터링된 후 값이 없으면 빈 리스트 반환
     */
    val selectedGaugeIds: Flow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[GAUGE_ORDER_KEY]
                ?.split(",")
                ?.filter { it in gaugeIdPool } // 유효한 ID만 걸러냄
                ?: emptyList()
        }

    /**
     * 주어진 ID를 토글(추가/제거)하는 함수
     *
     * @param id: 토글할 게이지 ID
     *  - 유효한 ID(gaugeIdPool에 속하지 않음)는 무시
     *  - 이미 리스트에 있으면 제거, 없으면 추가
     *  - 작업 전/중/후에 로깅 남김
     */
    suspend fun toggleGauge(id: String) {
        if (id !in gaugeIdPool) {
            Log.d(TAG, "toggleGauge: invalid id='$id', not in gaugeIdPool")
            return
        }
        Log.d(TAG, "toggleGauge: START id='$id'")

        // 멀티스레드 환경에서 목록 편집 시 충돌 방지를 위해 뮤텍스 잠금
        mutex.withLock {
            dataStore.edit { prefs ->
                val raw = prefs[GAUGE_ORDER_KEY]
                Log.d(TAG, "toggleGauge: prefs before edit raw='$raw'")

                // 현재 문자열을 분리하여 유효한 ID만 남긴 뒤, MutableList로 변환
                val cur = raw
                    ?.split(",")
                    ?.filter { it in gaugeIdPool }
                    ?.toMutableList()
                    ?: mutableListOf()
                Log.d(TAG, "toggleGauge: current list before change=$cur")

                if (id in cur) {
                    // 이미 리스트에 있으면 제거
                    cur.remove(id)
                    Log.d(TAG, "toggleGauge: removed '$id', new list=$cur")
                } else {
                    // 리스트에 없으면 추가
                    cur.add(id)
                    Log.d(TAG, "toggleGauge: added '$id', new list=$cur")
                }
                // 변경된 목록을 쉼표로 합쳐서 다시 저장
                val joined = cur.joinToString(",")
                prefs[GAUGE_ORDER_KEY] = joined
                Log.d(TAG, "toggleGauge: prefs after edit joined='$joined'")
            }
        }

        Log.d(TAG, "toggleGauge: END id='$id'")
    }

    /**
     * 인덱스 기반으로 순서 바꾸기(swap) 함수
     *
     * @param from: 원래 위치 인덱스
     * @param to: 바꾸려는 위치 인덱스
     *  - 인덱스가 동일하면 아무 동작 없음
     *  - 두 인덱스가 유효한 범위 내에 있어야 함
     */
    suspend fun swapGauge(from: Int, to: Int) {
        if (from == to) return
        // 뮤텍스로 동시성 제어
        mutex.withLock {
            dataStore.edit { prefs ->
                val cur = prefs[GAUGE_ORDER_KEY]
                    ?.split(",")
                    ?.filter { it in gaugeIdPool }
                    ?.toMutableList()
                    ?: return@edit

                // 인덱스 범위 검사
                if (from !in cur.indices || to !in cur.indices) return@edit

                // 실제 스왑
                cur.apply { Collections.swap(this, from, to) }
                prefs[GAUGE_ORDER_KEY] = cur.joinToString(",")
            }
        }
    }

    /**
     * 모든 게이지를 초기화(전체 선택)하는 함수
     *
     * - 현재 키 값을 gaugeIdPool 전체 목록으로 덮어씌움
     */
    suspend fun resetAllGauges() = mutex.withLock {
        dataStore.edit { prefs ->
            prefs[GAUGE_ORDER_KEY] = gaugeIdPool.joinToString(",")
        }
    }

    /* ---------- Device(디바이스 정보) 관련 ---------- */

    /**
     * 저장된 디바이스 정보를 Flow로 제공
     *
     * - SAVE_DEVICE_NAME, SAVE_DEVICE_ADDR 키가 모두 존재해야 ScannedDevice 객체 생성
     * - 둘 중 하나라도 없으면 null 반환
     */
    val getDevice: Flow<ScannedDevice?> = dataStore.data
        .map { prefs ->
            val name = prefs[SAVE_DEVICE_NAME]
            val addr = prefs[SAVE_DEVICE_ADDR]
            if (name != null && addr != null) ScannedDevice(name, addr) else null
        }

    /**
     * 디바이스 정보를 Preferences에 저장
     *
     * @param device: ScannedDevice(name, address) 객체
     */
    suspend fun saveDevice(device: ScannedDevice) {
        dataStore.edit { prefs ->
            prefs[SAVE_DEVICE_NAME] = device.name.orEmpty()
            prefs[SAVE_DEVICE_ADDR] = device.address
        }
    }

    /**
     * 저장된 블루투스 기기 정보를 삭제합니다.
     */
    suspend fun clearSavedDevice() {
        dataStore.edit { prefs ->
            prefs.remove(SAVE_DEVICE_NAME)
            prefs.remove(SAVE_DEVICE_ADDR)
        }
    }

    /* ---------- Auto Connect(충전 시 자동 연결) 관련 ---------- */

    /**
     * 충전 시 자동 연결 여부를 저장
     *
     * @param enabled: true면 자동 연결, false면 사용 안 함
     */
    suspend fun setAutoConnectOnCharge(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_CONNECT_KEY] = enabled
        }
    }

    /**
     * 충전 시 자동 연결 여부를 Flow로 제공
     *
     * - 키가 없으면 기본값 false 반환
     */
    val isAutoConnectOnCharge: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_CONNECT_KEY] ?: false }

    /* ---------- Connect Date(마지막 연결 날짜) 관련 ---------- */

    /**
     * 마지막 연결 날짜(문자열) 저장
     *
     * @param date: "yyyy-MM-dd" 형식 등 원하는 포맷의 문자열
     */
    suspend fun saveConnectData(date: String) {
        dataStore.edit { preferences ->
            preferences[CONNECT_DATE_KEY] = date
        }
    }

    /**
     * 마지막 연결 날짜를 Flow로 제공
     *
     * - 키가 없으면 빈 문자열("") 반환
     */
    fun getConnectDate(): Flow<String> {
        return dataStore.data
            .map { preferences ->
                preferences[CONNECT_DATE_KEY] ?: ""
            }
    }

    /* ---------- Driving Record(자동 주행 기록) 관련 ---------- */

    /**
     * 자동 주행 기록 여부를 저장
     *
     * @param value: true면 자동 주행 기록 활성화, false면 비활성화
     */
    suspend fun setDrivingRecode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_DRIVING_RECORD_ENABLED] = value
        }
    }

    /**
     * 자동 주행 기록 여부를 Flow로 제공
     *
     * - 키가 없거나 false인 경우 false 반환, true인 경우 true 반환
     */
    fun getDrivingRecodeSetDate(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                preferences[AUTO_DRIVING_RECORD_ENABLED] == true
            }
    }

    /**
     * 유류비를 저장합니다.
     *
     * @param cost: 저장할 유류비 (int, 최대 4자리까지 저장 가능)
     */
    suspend fun setFuelCost(cost: Int) {
        // 0~9999 범위인지 간단히 체크(원한다면 범위를 넘칠 때 예외 처리 또는 클램프 가능)
        val validCost = cost.coerceIn(0, 9999)
        dataStore.edit { prefs ->
            prefs[FUEL_COST_KEY] = validCost
        }
    }

    /**
     * 저장된 유류비를 Flow로 제공합니다.
     *
     * - 키가 없으면 기본값 0 반환
     * - Int형으로 최대 4자리까지 값을 저장/조회
     */
    val fuelCostFlow: Flow<Int> = dataStore.data
        .map { prefs ->
            prefs[FUEL_COST_KEY] ?: 1500
        }

    val isDtcDbInitialized: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[DTC_DB_INITIALIZED_KEY] ?: false }

    suspend fun setDtcDbInitialized(value: Boolean) {
        mutex.withLock {
            dataStore.edit { prefs ->
                prefs[DTC_DB_INITIALIZED_KEY] = value
            }
        }
    }
}