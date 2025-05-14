package com.devidea.aicar.ui.main.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.drive.DrivingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val drivingRepository: DrivingRepository,
) : ViewModel() {

//region 주행 세션 캘린더 및 기록

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    /** 세션 조회를 위한 현재 선택된 날짜입니다. */
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _month = MutableStateFlow(YearMonth.now())

    /** 캘린더 UI를 위한 현재 월 정보입니다. */
    val month: StateFlow<YearMonth> = _month

    /** 캘린더 월을 오프셋만큼 변경합니다. */
    fun changeMonth(delta: Int) {
        _month.value = _month.value.plusMonths(delta.toLong())
    }

    /** 선택된 날짜에 해당하는 세션 목록을 스트림으로 제공합니다. 날짜가 null이면 전체 세션을 반환합니다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<DrivingSession>> = _selectedDate
        .flatMapLatest { date ->
            if (date != null) drivingRepository.getSessionsByDate(date)
            else drivingRepository.getAllSessions()
        }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, emptyList())

    /** 현재 월에 세션이 있는 날짜들의 집합입니다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val markedDates: StateFlow<Set<LocalDate>> = _month
        .flatMapLatest { ym ->
            val startMillis =
                ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis =
                ym.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli() - 1
            drivingRepository.getSessionsInRange(startMillis, endMillis)
                .map { sessions ->
                    sessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
                        .toSet()
                }
        }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, emptySet())

    /** 모든 주행 세션을 가져옵니다. */
    fun getAllSessions(): Flow<List<DrivingSession>> = drivingRepository.getAllSessions()

    /** 저장된 모든 주행 세션을 삭제합니다. */
    fun deleteAllSessions() = viewModelScope.launch { drivingRepository.deleteAllSessions() }

    /** 지정한 ID의 세션을 삭제합니다. */
    fun deleteSession(sessionId: Long) =
        viewModelScope.launch { drivingRepository.deleteSession(sessionId) }

    /** 삭제된 세션을 복원합니다. */
    fun restoreSession(session: DrivingSession) =
        viewModelScope.launch { drivingRepository.insertSession(session) }

    /** 여러 세션을 한 번에 복원합니다. */
    fun restoreAllSessions(sessions: List<DrivingSession>) = viewModelScope.launch {
        sessions.forEach { drivingRepository.insertSession(it) }
    }

    /** 특정 세션의 상세 데이터 포인트를 가져옵니다. */
    fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>> =
        drivingRepository.getSessionData(sessionId)

    //endregion

    //region 재생 컨트롤

    private val _sliderPosition = MutableStateFlow(0f)

    /** 재생 슬라이더의 현재 위치 (0f..1f) 입니다. */
    val sliderPosition: StateFlow<Float> = _sliderPosition

    private val _isPlaying = MutableStateFlow(false)

    /** 재생 애니메이션이 실행 중이면 true입니다. */
    val isPlaying: StateFlow<Boolean> = _isPlaying

    /** 사용자가 슬라이더를 드래그할 때 위치를 업데이트합니다. */
    fun updateSlider(position: Float) {
        _sliderPosition.value = position
    }

    /** 재생/일시정지 상태를 토글합니다. */
    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    /** 캘린더에서 특정 날짜를 선택합니다. */
    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date ?: LocalDate.now()
    }

    //endregion
}