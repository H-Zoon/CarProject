package com.devidea.chevy.bluetooth

import com.devidea.chevy.Logger
import com.devidea.chevy.service.SppClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/*class ODBHelper @Inject constructor(private val sppClient: SppClientImpl) {

    *//**
     * 엔진(파워트레인) DTC 조회 (Mode 03).
     * @return 고장 코드 목록 (예: ["P0301","P0420"]) 또는 빈 리스트
     *//*
    suspend fun getEngineDtcCodes(): List<String> = withContext(Dispatchers.IO) {
        // 1) Mode03 요청
        val raw = sppClient.sendMessageAndGetResponse("03")
            .replace("[^0-9A-Fa-f ]".toRegex(), "")  // 헥사 + 공백만 남기기
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }

        // 2) 응답 예시: ["43","01","0C","00","00", …]
        if (raw.isEmpty() || raw[0] != "43") {
            // 43 == 응답(Mode+0x40)
            return@withContext emptyList()
        }

        // 3) 데이터 바이트(A,B) 블록을 2개씩 읽어서 파싱
        val codes = mutableListOf<String>()
        val dataBytes = raw.drop(1)  // 첫 항목(43) 제외
        for (i in dataBytes.indices step 2) {
            if (i + 1 >= dataBytes.size) break
            val hi = dataBytes[i].toIntOrNull(16) ?: continue
            val lo = dataBytes[i + 1].toIntOrNull(16) ?: continue

            // A 바이트: hi, B 바이트: lo
            val typeNibble = (hi shr 6) and 0x3
            val typeChar = when (typeNibble) {
                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                3 -> 'U'
                else -> '?'
            }
            val digit1 = (hi shr 4) and 0x3
            val digit2 = hi and 0xF
            val digit3 = (lo shr 4) and 0xF
            val digit4 = lo and 0xF

            val code = StringBuilder()
                .append(typeChar)
                .append(digit1)
                .append(digit2.toString(16).uppercase())
                .append(digit3.toString(16).uppercase())
                .append(digit4.toString(16).uppercase())
                .toString()
            codes += code
        }

        return@withContext codes
    }

    private val AFR = 14.7               // 공기대연료비
    private val FUEL_DENSITY = 720.0     // 휘발유 밀도

    // ① 실시간 연비를 흘려줄 SharedFlow
    private val _economyFlow = MutableSharedFlow<Double>(replay = 1)
    val economyFlow: SharedFlow<Double> = _economyFlow.asSharedFlow()

    // ② 모니터링을 돌릴 스코프 / 잡
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    fun startFuelEconomyMonitor() {
        monitorJob?.cancel()
        monitorJob = monitorScope.launch {
            // 1) PID 0x5E 지원 여부 확인
            val supportByMask = isPidSupported(0x5E)
            Logger.d { "PID 0x5E 지원 (마스크 확인): $supportByMask" }

            // 2) 직접 읽어보기 (예외나 null 처리로 판단)
            val supportByRead = try {
                getFuelRatePid() != null
            } catch (_: Exception) {
                false
            }
            Logger.d { "PID 0x5E 지원 (직접 읽기): $supportByRead" }

            val usePid5E = supportByMask && supportByRead

            if (!usePid5E) {
                Logger.w { "PID 0x5E 미지원, MAF+RPM 방식으로 폴백" }
            }

            // 3) 주기적으로 연비 계산
            while (true) {
                val speed = getSpeedPid()
                val fuelRateLh = if (usePid5E) {
                    getFuelRatePid()
                } else {
                    val maf = getMafPid()
                    val mafLh = maf?.let { it * 3600 / (AFR * FUEL_DENSITY) }
                    mafLh
                }

                if (speed != null && fuelRateLh != null && fuelRateLh > 0) {
                    val economy = speed.toDouble() / fuelRateLh
                    Logger.d {
                        "실시간 연비: ${"%.2f".format(economy)} km/L (speed=${speed}km/h rate=${
                            "%.2f".format(
                                fuelRateLh
                            )
                        }L/h)"
                    }
                    _economyFlow.emit(economy)
                } else {
                    Logger.d { "연비 계산 실패 (speed=$speed, rate=$fuelRateLh)" }
                }

                delay(1000)  // 1초 간격
            }
        }
    }

    fun stopFuelEconomyMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }


    suspend fun isPidSupported(pid: Int): Boolean = withContext(Dispatchers.IO) {
        // 1) 어떤 마스크를 읽어야 할지 결정
        val maskPid = when (pid) {
            in 0x01..0x20 -> 0x00
            in 0x21..0x40 -> 0x20
            in 0x41..0x60 -> 0x40
            else -> return@withContext false
        }
        val rawResp = sppClient.sendMessageAndGetResponse(String.format("01%02X", maskPid))
        // 3) 응답에서 4바이트 데이터만 분리 (“41 XX AA BB CC DD”)
        val bytes = rawResp
            .replace("[^0-9A-Fa-f ]".toRegex(), "")
            .split("\\s+".toRegex())
            .mapNotNull { it.toIntOrNull(16)?.toByte() }
            .drop(2)  // AA부터

        if (bytes.size < 4) return@withContext false

        // 4) 비트 위치 계산
        val bitIndex = (pid - maskPid - 1)
        val byteIndex = bitIndex / 8
        val bitMask = (1 shl (7 - (bitIndex % 8))).toByte()

        // 5) 지원 여부 반환
        return@withContext (bytes[byteIndex].toInt() and bitMask.toInt()) != 0
    }


    private suspend fun getSpeedPid(): Int? = withContext(Dispatchers.IO) {
        val resp = sppClient.sendMessageAndGetResponse("010D")
        // “41 0D AA” -> AA
        resp.split("\\s+".toRegex())
            .takeIf { it.size >= 3 && it[0] == "41" && it[1] == "0D" }
            ?.get(2)
            ?.toIntOrNull(16)
    }

    private suspend fun getFuelRatePid(): Double? = withContext(Dispatchers.IO) {
        val resp = sppClient.sendMessageAndGetResponse("015E")
        // “41 5E AA BB” -> (A*256+B)/20
        val parts = resp.split("\\s+".toRegex())
        if (parts.size >= 4 && parts[0] == "41" && parts[1] == "5E") {
            val a = parts[2].toIntOrNull(16) ?: return@withContext null
            val b = parts[3].toIntOrNull(16) ?: return@withContext null
            (a * 256 + b) / 20.0
        } else null
    }

    private suspend fun getMafPid(): Double? = withContext(Dispatchers.IO) {
        val resp = sppClient.sendMessageAndGetResponse("0110")
        // “41 10 AA BB” -> (A*256+B)/100 g/s
        val parts = resp.split("\\s+".toRegex())
        if (parts.size >= 4 && parts[0] == "41" && parts[1] == "10") {
            val a = parts[2].toIntOrNull(16) ?: return@withContext null
            val b = parts[3].toIntOrNull(16) ?: return@withContext null
            (a * 256 + b) / 100.0
        } else null
    }
}*/
