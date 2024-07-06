package com.devidea.chevy.carsystem

import android.annotation.SuppressLint
import android.util.Log

class PID {
    var strName: String = ""
    var strValue: String = ""
}

class OBDData {
    var isFastDetectionShowAll = false
    private val mDetectedDataList: MutableList<String> = mutableListOf()
    private val mPidDataList: MutableList<PID> = ArrayList()

    @SuppressLint("UseSparseArrays")
    private val mPidMap: HashMap<Int, XJPID> = HashMap()

    @SuppressLint("UseSparseArrays")
    val mSupportPIDMap: HashMap<Int, XJPIDDATA> = HashMap()
    var m_strBuDingPidIndex25 = ""

    init {
        initPidMap()
        initPidDataMap()
    }

    fun getPidDataList(): List<PID> {
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
        mPidMap[68] = XJPID(68, 51, "대기압“, “Kpa")
        mPidMap[69] = XJPID(69, 52, "Bank1 산소 센서1 당량비")
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

    inner class XJPID {
        var number = 0
        var PID = 0
        var unit = ""
        var discription = ""
        var status = "--|--"

        constructor()
        constructor(number: Int, PID: Int, discription: String, unit: String = "") {
            this.number = number
            this.PID = PID
            this.discription = discription
            this.unit = unit
        }
    }

    inner class XJPIDDATA {
        var A = 0
        var B = 0
        var C = 0
        var D = 0
        var support = -1
    }

    fun handlePid(i: Int, b: Byte, b2: Byte, b3: Byte, b4: Byte) {
        Log.e("dyt", "start")
        val i2 = b.toInt() and 255
        val i3 = i + 0
        var i4 = 7
        while (i4 >= 0) {
            val i6 = (7 - i4) + i3
            Log.e(" dyt ", "1 id = $i6")
            val xjpiddata = mSupportPIDMap[i6]
            xjpiddata?.support = if ((1 shl i4) and i2 == 0) 0 else 1
            i4--
        }
        val i7 = b2.toInt() and 255
        val i8 = i3 + 8
        for (i9 in 7 downTo 0) {
            val i10 = (7 - i9) + i8
            Log.e(" dyt ", "2 id = $i10")
            mSupportPIDMap[i10]?.support = if ((1 shl i9) and i7 != 0) 1 else 0
        }
        val i11 = b3.toInt() and 255
        val i12 = i8 + 8
        for (i13 in 7 downTo 0) {
            val i14 = (7 - i13) + i12
            Log.e(" dyt ", "3 id = $i14")
            mSupportPIDMap[i14]?.support = if ((1 shl i13) and i11 != 0) 1 else 0
        }
        val i15 = b4.toInt() and 255
        val i16 = i12 + 8
        for (i17 in 7 downTo 0) {
            val i18 = (7 - i17) + i16
            Log.e(" dyt ", "4 id = $i18")
            mSupportPIDMap[i18]?.support = if ((1 shl i17) and i15 != 0) 1 else 0
        }
    }

    fun getDectectedErrObdList(): MutableList<String> {
        return mDetectedDataList
    }
}