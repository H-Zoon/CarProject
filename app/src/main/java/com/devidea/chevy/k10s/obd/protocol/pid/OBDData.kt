package com.devidea.chevy.k10s.obd.protocol.pid

import android.annotation.SuppressLint
import com.devidea.chevy.Logger


class OBDData {
    val TAG = "OBDData"
    private var isFastDetectionShowAll: Boolean = true
    private val mDetectedDataList: MutableList<String> = mutableListOf()
    private val mPidDataList: MutableList<PIDListData> = ArrayList()
    private val mPidMap: HashMap<Int, XJPID> = HashMap()
    val mSupportPIDMap: HashMap<Int, XJPIDDATA> = HashMap()
    var m_strBuDingPidIndex25 = ""

    init {
        initPidMap()
        initPidDataMap()
    }

    fun getPidDataList(): MutableList<PIDListData> {
        return mPidDataList
    }


    private fun initPidMap() {
        mPidMap.clear()
        mPidMap[1] = XJPID(1, 1, "고장 코드 수")
        mPidMap[2] = XJPID(2, 1, "MIL 상태")
        mPidMap[3] = XJPID(3, 1, "MIL 상태")
        mPidMap[4] = XJPID(4, 1, "연료 시스템 모니터링 지원 여부")
        mPidMap[5] = XJPID(5, 1, "구성 요소 시스템 모니터링 지원 여부")
        mPidMap[6] = XJPID(6, 1, "실화 모니터링 완료 여부")
        mPidMap[7] = XJPID(7, 1, "연료 시스템 모니터링 완료 여부")
        mPidMap[8] = XJPID(8, 1, "구성 요소 시스템 모니터링 완료 여부")
        mPidMap[9] = XJPID(9, 1, "촉매 모니터링 지원 여부")
        mPidMap[10] = XJPID(10, 1, "촉매 가열 모니터링 지원 여부")
        mPidMap[11] = XJPID(11, 1, "증발 시스템 모니터링 지원 여부")
        mPidMap[12] = XJPID(12, 1, "2차 공기 시스템 모니터링 지원 여부")
        mPidMap[13] = XJPID(13, 1, "에어컨 시스템 냉각 모니터링 지원 여부")
        mPidMap[14] = XJPID(14, 1, "산소 센서 모니터링 지원 여부")
        mPidMap[15] = XJPID(15, 1, "산소 센서 가열 모니터링 지원 여부")
        mPidMap[16] = XJPID(16, 1, "EGR 시스템 모니터링 지원 여부")
        mPidMap[17] = XJPID(17, 1, "촉매 모니터링 완료 여부")
        mPidMap[18] = XJPID(18, 1, "촉매 가열 모니터링 완료 여부")
        mPidMap[19] = XJPID(19, 1, "증발 시스템 모니터링 완료 여부")
        mPidMap[20] = XJPID(20, 1, "2차 공기 시스템 모니터링 완료 여부")
        mPidMap[21] = XJPID(21, 1, "공기 시스템 냉각 모니터링 완료 여부")
        mPidMap[22] = XJPID(22, 1, "산소 센서 모니터링 완료 여부")
        mPidMap[23] = XJPID(23, 1, "산소 센서 가열 모니터링 완료 여부")
        mPidMap[24] = XJPID(24, 1, "EGR 시스템 모니터링 완료 여부")
        mPidMap[25] = XJPID(25, 3, "연료 시스템 1 상태")
        mPidMap[26] = XJPID(26, 3, "연료 시스템 2 상태")
        mPidMap[27] = XJPID(27, 4, "계산된 부하 값", "%")
        mPidMap[28] = XJPID(28, 5, "엔진 냉각수 온도", "°C")
        mPidMap[29] = XJPID(29, 6, "단기 연료 보정 - Bank1", "%")
        mPidMap[30] = XJPID(30, 7, "장기 연료 보정 - Bank1", "°C")
        mPidMap[31] = XJPID(31, 8, "단기 연료 보정 - Bank2", "°C")
        mPidMap[32] = XJPID(32, 9, "장기 연료 보정 - Bank2", "°C")
        mPidMap[33] = XJPID(33, 10, "연료 압력", "Kpa")
        mPidMap[34] = XJPID(34, 11, "흡기 매니폴드 절대 압력", "Kpa")
        mPidMap[35] = XJPID(35, 12, "엔진 회전수", "rpm")
        mPidMap[36] = XJPID(36, 13, "차량 속도", "Km/h")
        mPidMap[37] = XJPID(37, 14, "점화 타이밍 조정 각도", "도")
        mPidMap[38] = XJPID(38, 15, "흡기 온도", "°C")
        mPidMap[39] = XJPID(39, 16, "공기 유량", "Grams/Sec")
        mPidMap[40] = XJPID(40, 17, "스로틀 절대 위치", "%")
        mPidMap[41] = XJPID(41, 20, "Bank1 센서1 산소 센서 출력 전압", "V")
        mPidMap[42] = XJPID(42, 20, "Bank1 센서1 단기 연료 보정", "%")
        mPidMap[43] = XJPID(43, 21, "Bank1 센서2 산소 센서 출력 전압", "V")
        mPidMap[44] = XJPID(44, 21, "Bank1 센서2 단기 연료 보정", "%")
        mPidMap[45] = XJPID(45, 22, "Bank1 센서3 산소 센서 출력 전압", "V")
        mPidMap[46] = XJPID(46, 22, "Bank1 센서3 단기 연료 보정", "%")
        mPidMap[47] = XJPID(47, 23, "Bank1 센서4 산소 센서 출력 전압", "V")
        mPidMap[48] = XJPID(48, 23, "Bank1 센서4 단기 연료 보정", "%")
        mPidMap[49] = XJPID(49, 24, "Bank2 센서1 산소 센서 출력 전압", "V")
        mPidMap[50] = XJPID(50, 24, "Bank2 센서1 단기 연료 보정", "%")
        mPidMap[51] = XJPID(51, 25, "Bank2 센서2 산소 센서 출력 전압", "V")
        mPidMap[52] = XJPID(52, 25, "Bank2 센서2 단기 연료 보정", "%")
        mPidMap[53] = XJPID(53, 26, "Bank2 센서3 산소 센서 출력 전압", "V")
        mPidMap[54] = XJPID(54, 26, "Bank2 센서3 단기 연료 보정", "%")
        mPidMap[55] = XJPID(55, 27, "Bank2 센서4 산소 센서 출력 전압", "V")
        mPidMap[56] = XJPID(56, 27, "Bank2 센서4 단기 연료 보정", "%")
        mPidMap[57] = XJPID(57, 31, "엔진 시동 후 작동 시간", "S")
        mPidMap[58] = XJPID(58, 33, "MIL 활성화 상태에서 주행 거리", "KM")
        mPidMap[59] = XJPID(59, 34, "연료 레일 압력/매니폴드 진공 압력 대비", "Kpa")
        mPidMap[60] = XJPID(60, 35, "연료 레일 압력", "Kpa")
        mPidMap[61] = XJPID(61, 44, "지시된 EGR", "%")
        mPidMap[62] = XJPID(62, 45, "반환된 EGR 오류", "%")
        mPidMap[63] = XJPID(63, 46, "연료 증발 방출 제어 명령", "%")
        mPidMap[64] = XJPID(64, 47, "잔여 연료량", "%")
        mPidMap[65] = XJPID(65, 48, "고장 코드 삭제 후 예열 횟수")
        mPidMap[66] = XJPID(66, 49, "고장 코드 삭제 후 주행 거리", "KM")
        mPidMap[67] = XJPID(67, 50, "증발 시스템 증발 압력", "Pa")
        mPidMap[68] = XJPID(68, 51, "대기압", "Kpa")
        mPidMap[69] = XJPID(69, 52, "Bank1 산소 센서1 당량비", "Ma")
        mPidMap[70] = XJPID(70, 52, "Bank1 산소 센서1 전류", "Ma")
        mPidMap[71] = XJPID(71, 53, "Bank1 산소 센서2 당량비")
        mPidMap[72] = XJPID(72, 53, "Bank1 산소 센서2 전류", "Ma")
        mPidMap[73] = XJPID(73, 54, "Bank1 산소 센서3 당량비")
        mPidMap[74] = XJPID(74, 54, "Bank1 산소 센서3 전류", "Ma")
        mPidMap[75] = XJPID(75, 55, "Bank1 산소 센서4 당량비")
        mPidMap[76] = XJPID(76, 55, "Bank1 산소 센서4 전류", "Ma")
        mPidMap[77] = XJPID(77, 56, "Bank2 산소 센서1 당량비")
        mPidMap[78] = XJPID(78, 56, "Bank2 산소 센서1 전류", "Ma")
        mPidMap[79] = XJPID(79, 57, "Bank2 산소 센서2 당량비")
        mPidMap[80] = XJPID(80, 57, "Bank2 산소 센서2 전류", "Ma")
        mPidMap[81] = XJPID(81, 58, "Bank2 산소 센서3 당량비")
        mPidMap[82] = XJPID(82, 58, "Bank2 산소 센서3 전류", "Ma")
        mPidMap[83] = XJPID(83, 59, "Bank2 산소 센서4 당량비")
        mPidMap[84] = XJPID(84, 59, "Bank2 산소 센서4 전류", "Ma")
        mPidMap[85] = XJPID(85, 60, "촉매 온도 Bank1 센서1", "°C")
        mPidMap[86] = XJPID(86, 61, "촉매 온도 Bank2 센서1", "°C")
        mPidMap[87] = XJPID(87, 62, "촉매 온도 Bank1 센서2", "°C")
        mPidMap[88] = XJPID(88, 63, "촉매 온도 Bank2 센서2", "°C")
        mPidMap[89] = XJPID(89, 66, "제어 모듈 전압", "V")
        mPidMap[90] = XJPID(90, 67, "절대 부하 값", "%")
        mPidMap[91] = XJPID(91, 68, "당량비 제어 명령", "%")
        mPidMap[92] = XJPID(92, 69, "스로틀 상대 위치", "%")
        mPidMap[93] = XJPID(93, 70, "주변 온도", "°C")
        mPidMap[94] = XJPID(94, 71, "스로틀 절대 위치 B", "%")
        mPidMap[95] = XJPID(95, 72, "스로틀 절대 위치 C", "%")
        mPidMap[96] = XJPID(96, 73, "가속 페달 위치 D", "%")
        mPidMap[97] = XJPID(97, 74, "가속 페달 위치 E", "%")
        mPidMap[98] = XJPID(98, 75, "가속 페달 위치 F", "%")
        mPidMap[99] = XJPID(99, 76, "스로틀 액추에이터 제어 명령", "%")
        mPidMap[100] = XJPID(100, 77, "MIL 램프 점등 후 엔진 작동 시간", "minutes")
        mPidMap[101] = XJPID(101, 78, "고장 코드 초기화 후 엔진 작동 시간", "minutes")
        mPidMap[102] = XJPID(102, 80, "공기 유량 센서 최대 값", "g/s")
        mPidMap[103] = XJPID(103, 83, "연료 증발 방출 제어 시스템 증기 압력 절대 값", "Kpa")
        mPidMap[104] = XJPID(104, 84, "연료 증발 방출 제어 시스템 증기 압력", "pa")
        mPidMap[105] = XJPID(105, 85, "2차 산소 센서 단기 연료 보정 Bank1", "%")
        mPidMap[106] = XJPID(106, 85, "2차 산소 센서 단기 연료 보정 Bank3", "%")
        mPidMap[107] = XJPID(107, 86, "2차 산소 센서 장기 연료 보정 Bank1", "%")
        mPidMap[108] = XJPID(108, 86, "2차 산소 센서 장기 연료 보정 Bank3", "%")
        mPidMap[109] = XJPID(109, 87, "2차 산소 센서 단기 연료 보정 Bank2", "%")
        mPidMap[110] = XJPID(110, 87, "2차 산소 센서 단기 연료 보정 Bank4", "%")
        mPidMap[111] = XJPID(111, 88, "2차 산소 센서 장기 연료 보정 Bank2", "%")
        mPidMap[112] = XJPID(112, 88, "2차 산소 센서 장기 연료 보정 Bank4", "%")
        mPidMap[113] = XJPID(113, 89, "연료 레일 압력", "Kpa")
        mPidMap[114] = XJPID(114, 90, "가속 페달 상대 위치", "%")
        mPidMap[115] = XJPID(115, 91, "하이브리드 배터리 팩 잔여 용량", "%")
        mPidMap[116] = XJPID(116, 92, "엔진 윤활유 온도", "°C")
        mPidMap[117] = XJPID(117, 93, "연료 분사 타이밍")
        mPidMap[118] = XJPID(118, 94, "엔진 연료 소비율", "L/H")
    }

    private fun initPidDataMap() {
        mSupportPIDMap.clear()
        for (i in 0..255) {
            mSupportPIDMap[i] = XJPIDDATA()
        }
    }

    fun handlePid(basePidIndex: Int, byte1: Byte, byte2: Byte, byte3: Byte, byte4: Byte) {
        val firstByteInt = byte1.toInt() and 255
        var bitPositions = 7

        // 처리 첫 번째 바이트 (byte1)
        while (bitPositions >= 0) {
            val pidIndex = (7 - bitPositions) + basePidIndex
            Logger.d { "1 id = $pidIndex" }
            val pidData = mSupportPIDMap[pidIndex]
            pidData?.support = if ((1 shl bitPositions) and firstByteInt == 0) 0 else 1
            bitPositions--
        }

        val secondByteInt = byte2.toInt() and 255
        val secondBaseIndex = basePidIndex + 8
        for (bitPosition in 7 downTo 0) {
            val pidIndex = (7 - bitPosition) + secondBaseIndex
            Logger.d { "2 id = $pidIndex" }
            mSupportPIDMap[pidIndex]?.support =
                if ((1 shl bitPosition) and secondByteInt != 0) 1 else 0
        }

        val thirdByteInt = byte3.toInt() and 255
        val thirdBaseIndex = secondBaseIndex + 8
        for (bitPosition in 7 downTo 0) {
            val pidIndex = (7 - bitPosition) + thirdBaseIndex
            Logger.d { "3 id = $pidIndex" }
            mSupportPIDMap[pidIndex]?.support =
                if ((1 shl bitPosition) and thirdByteInt != 0) 1 else 0
        }

        val fourthByteInt = byte4.toInt() and 255
        val fourthBaseIndex = thirdBaseIndex + 8
        for (bitPosition in 7 downTo 0) {
            val pidIndex = (7 - bitPosition) + fourthBaseIndex
            Logger.d { "4 id = $pidIndex" }
            mSupportPIDMap[pidIndex]?.support =
                if ((1 shl bitPosition) and fourthByteInt != 0) 1 else 0
        }
    }

    fun getDectectedErrObdList(): MutableList<String> {
        return mDetectedDataList
    }

    @SuppressLint("DefaultLocale")
    fun showPid() {
        val tempPIDList: MutableList<*> = this.mPidDataList
        tempPIDList.clear()

        for (pids in 1..118) {
            val tempPIDMap = mPidMap[pids]
            if (tempPIDMap == null) {
                Logger.d { "pidMap에서 찾을 수 없음" }
                return
            }

            val tempSupportMap = mSupportPIDMap[tempPIDMap.PID]
            if (tempSupportMap == null) {
                Logger.d { "mSupportPIDMap에서 찾을 수 없음" }
                return
            }

            var mapA = tempSupportMap.A
            var mapB = tempSupportMap.B
            var mapC = tempSupportMap.C
            val mapD = tempSupportMap.D
            var result = "VALID"

            if (tempSupportMap.support == 1) {
                when (tempPIDMap.number) {
                    1 -> result = String.format("%d", mapA and 127)
                    2 -> result = if (1.0f == (mapA shr 7 and 1).toFloat()) "켜짐" else "꺼짐"
                    3 -> if (1.0f == (mapB shl 7 shr 7 and 1).toFloat()) "예" else "아니오"
                    4 -> if (1.0f == (mapB shl 6 shr 7 and 1).toFloat()) "예" else "아니오"
                    5 -> if (1.0f == (mapB shl 5 shr 7 and 1).toFloat()) "예" else "아니오"
                    6 -> if (1.0f != (mapB shl 3 shr 7 and 1).toFloat()) "아니오" else "예"
                    7 -> if (1.0f != (mapB shl 2 shr 7 and 1).toFloat()) "아니오" else "예"
                    8 -> if (1.0f != (mapB shl 1 shr 7 and 1).toFloat()) "아니오" else "예"
                    9 -> if (1.0f == (mapC shl 7 shr 7 and 1).toFloat()) "예" else "아니오"
                    10 -> if (1.0f == (mapC shl 6 shr 7 and 1).toFloat()) "예" else "아니오"
                    11 -> if (1.0f == (mapC shl 5 shr 7 and 1).toFloat()) "예" else "아니오"
                    12 -> if (1.0f == (mapC shl 4 shr 7 and 1).toFloat()) "예" else "아니오"
                    13 -> if (1.0f == (mapC shl 3 shr 7 and 1).toFloat()) "예" else "아니오"
                    14 -> if (1.0f == (mapC shl 2 shr 7 and 1).toFloat()) "예" else "아니오"
                    15 -> if (1.0f == (mapC shl 1 shr 7 and 1).toFloat()) "예" else "아니오"
                    16 -> if (1.0f == (mapC shr 7 and 1).toFloat()) "예" else "아니오"
                    17 -> if (1.0f == (mapD shl 7 shr 7 and 1).toFloat()) "예" else "아니오"
                    18 -> if (1.0f == (mapD shl 6 shr 7 and 1).toFloat()) "예" else "아니오"
                    19 -> if (1.0f == (mapD shl 5 shr 7 and 1).toFloat()) "예" else "아니오"
                    20 -> if (1.0f == (mapD shl 4 shr 7 and 1).toFloat()) "예" else "아니오"
                    21 -> if (1.0f == (mapD shl 3 shr 7 and 1).toFloat()) "예" else "아니오"
                    22 -> if (1.0f == (mapD shl 2 shr 7 and 1).toFloat()) "예" else "아니오"
                    23 -> if (1.0f == (mapD shl 1 shr 7 and 1).toFloat()) "예" else "아니오"
                    24 -> if (1.0f == (mapD shr 7 and 1).toFloat()) "예" else "아니오"
                    25 -> {
                        mapB = mapA and 1
                        mapC = (mapA and 2) shr 1
                        result = when {
                            mapC == 1 -> "폐쇄 루프"
                            mapB == 1 -> "개방 루프"
                            (mapA and 4) shr 2 == 1 -> "개방 루프 제어"
                            (mapA and 8) shr 3 == 1 -> "개방 루프 실패"
                            (mapA and 16) shr 4 == 1 -> "폐쇄 루프 실패"
                            else -> "VALID"
                        }
                        this.m_strBuDingPidIndex25 = result
                    }

                    26 -> {
                        mapC = mapB and 1
                        mapA = (mapB and 2) shr 1
                        result = when {
                            mapA == 1 -> "폐쇄 루프"
                            mapC == 1 -> "개방 루프"
                            (mapB and 4) shr 2 == 1 -> "개방 루프 제어"
                            (mapB and 8) shr 3 == 1 -> "개방 루프 실패"
                            (mapB and 16) shr 4 == 1 -> "폐쇄 루프 실패"
                            else -> m_strBuDingPidIndex25
                        }
                    }

                    27 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    28 -> result =
                        String.format("%.1f%s", (mapA.toDouble() - 40.0).toFloat(), tempPIDMap.description)

                    29 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    30 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    31 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    32 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    33 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 3.0).toFloat(), tempPIDMap.description)

                    34 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 1.0).toFloat(), tempPIDMap.description)

                    35 -> result = String.format(
                        "%d%s",
                        ((mapA * 256 + mapB) / 4).toFloat().toInt(),
                        tempPIDMap.description
                    )

                    36 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 1.0).toFloat(), tempPIDMap.description)

                    37 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) / 2.0).toFloat(),
                        tempPIDMap.description
                    )

                    38 -> result =
                        String.format("%.1f%s", (mapA.toDouble() - 40.0).toFloat(), tempPIDMap.description)

                    39 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 100.0).toFloat(),
                        tempPIDMap.description
                    )

                    40 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    41 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    42 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    43 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    44 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    45 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    46 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    47 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    48 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    49 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    50 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    51 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    52 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    53 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    54 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    55 -> result =
                        String.format(
                            "%.1f%s",
                            (mapA.toDouble() / 200.0).toFloat(),
                            tempPIDMap.description
                        )

                    56 -> result = if (mapB == 255) "센서 미사용" else String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    57 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 1.0).toFloat(),
                        tempPIDMap.description
                    )

                    58 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 1.0).toFloat(),
                        tempPIDMap.description
                    )

                    59 -> result = String.format(
                        "%.3f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 0.079).toFloat(),
                        tempPIDMap.description
                    )

                    60 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 10.0).toFloat(),
                        tempPIDMap.description
                    )

                    61 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    62 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    63 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    64 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    65 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 1.0).toFloat(), tempPIDMap.description)

                    66 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 256.0 + mapB.toDouble()).toFloat(),
                        tempPIDMap.description
                    )

                    67 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toChar().code.toDouble() * 256.0 + mapB.toChar().code.toDouble()) / 4.0).toFloat(),
                        tempPIDMap.description
                    )

                    68 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 1.0).toFloat(), tempPIDMap.description)

                    69 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    70 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    71 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    72 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    73 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    74 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    75 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    76 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    77 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    78 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    79 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 3276.0).toFloat(),
                        tempPIDMap.description
                    )

                    80 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    81 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    82 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    83 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    84 -> result = String.format(
                        "%.1f%s",
                        ((mapC.toDouble() * 256.0 + mapD.toDouble()) / 256.0 - 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    85 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 10.0 - 40.0).toFloat(),
                        tempPIDMap.description
                    )

                    86 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 10.0 - 40.0).toFloat(),
                        tempPIDMap.description
                    )

                    87 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 10.0 - 40.0).toFloat(),
                        tempPIDMap.description
                    )

                    88 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 10.0 - 40.0).toFloat(),
                        tempPIDMap.description
                    )

                    89 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 1000.0).toFloat(),
                        tempPIDMap.description
                    )

                    90 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    91 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 32768.0).toFloat(),
                        tempPIDMap.description
                    )

                    92 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    93 -> result =
                        String.format("%.1f%s", (mapA.toDouble() - 40.0).toFloat(), tempPIDMap.description)

                    94 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    95 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    96 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    97 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    98 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    99 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    100 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 1.0).toFloat(),
                        tempPIDMap.description
                    )

                    101 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 1.0).toFloat(),
                        tempPIDMap.description
                    )

                    102 -> result =
                        String.format("%.1f%s", (mapA.toDouble() * 10.0).toFloat(), tempPIDMap.description)

                    103 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) / 200.0).toFloat(),
                        tempPIDMap.description
                    )

                    104 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble() - 32767.0) * 1.0).toFloat(),
                        tempPIDMap.description
                    )

                    105 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    106 -> result = String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    107 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    108 -> result = String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    109 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    110 -> result = String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    111 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    112 -> result = String.format(
                        "%.1f%s",
                        ((mapB.toDouble() - 128.0) * 100.0 / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    113 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 10.0).toFloat(),
                        tempPIDMap.description
                    )

                    114 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    115 -> result = String.format(
                        "%.1f%s",
                        (mapA.toDouble() * 100.0 / 255.0).toFloat(),
                        tempPIDMap.description
                    )

                    116 -> result =
                        String.format("%.1f%s", (mapA.toDouble() - 40.0).toFloat(), tempPIDMap.description)

                    117 -> result = String.format(
                        "%.1f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble() - 26880.0) / 128.0).toFloat(),
                        tempPIDMap.description
                    )

                    118 -> result = String.format(
                        "%.2f%s",
                        ((mapA.toDouble() * 256.0 + mapB.toDouble()) * 0.05).toFloat(),
                        tempPIDMap.description
                    )

                    else -> result = "알 수 없음"
                }
            } else if (tempSupportMap.support == 0) {
                result = "지원 안 함"
            } else {
                result = "VALID"
                if (tempSupportMap.support == -1) {
                    result = "--|--"
                }
            }

            tempPIDMap.status = result
            val var15: PIDListData
            if (this.isFastDetectionShowAll) {
                var15 = PIDListData()
                var15.strName = String.format("%d %s", tempPIDMap.number, tempPIDMap.unit)
                var15.strValue = tempPIDMap.status
                mPidDataList.add(var15)
            } else if (tempSupportMap.support == 1) {
                var15 = PIDListData()
                var15.strName = String.format("%d %s", tempPIDMap.number, tempPIDMap.unit)
                var15.strValue = tempPIDMap.status
                mPidDataList.add(var15)
            }
        }
    }
}