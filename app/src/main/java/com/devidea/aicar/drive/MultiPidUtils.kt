package com.devidea.aicar.drive

import java.util.Locale

/**
 * MultiPID 요청/응답 유틸
 */
object MultiPidUtils {
    private const val MODE = "01" // Mode 01만 지원
    internal const val MAX_PIDS = 5 // CAN 페이로드 한계

    /**
     * 주어진 PID 리스트로 요청 문자열(“01 0C 0D 05 …”)을 만듭니다.
     * - PIDs.code는 “01XX” 형태이므로, 앞 두 글자(“01”)를 제거한 뒤 모아서 사용
     * - 7개 이상의 PID를 넣으면 IllegalArgumentException
     */
    fun buildCommand(pids: List<PIDs>): String {
        require(pids.all { it.code.startsWith(MODE) }) {
            "Multi-PID 요청은 Mode01(PIDs.code가 '01'로 시작)만 지원합니다."
        }
        require(pids.size in 1..MAX_PIDS) {
            "한 프레임에 최대 $MAX_PIDS 개의 PID만 요청할 수 있습니다."
        }

        // "01" + pidBytes.joinToString(" ")
        val pidBytes = pids.map { it.code.substring(2) } // e.g. "0C", "0D", "05"
        return (MODE + pidBytes.joinToString("") { it })
            .chunked(2)
            .joinToString(" ") // e.g. "01 0C 0D 05"
    }

    /**
     * ECU에서 받은 응답(예: "07 41 0C 1AF8 0D28 05 3C")
     * 를 파싱해서 Map<PID, 값> 형태로 반환합니다.
     */

    fun parseResponse(resp: String): Map<PIDs, Number> {
        // 1) 공백 제거 및 대문자화
        val compact = resp.replace("\\s".toRegex(), "").uppercase(Locale.US)
        require(compact.startsWith("41")) { "Mode01 응답이 아닙니다: ${compact.substring(0, 2)}" }

        val result = mutableMapOf<PIDs, Number>()
        var hexOffset = 2 // mode 바이트(2 hex chars) 바로 다음 위치

        while (hexOffset < compact.length) {
            // 2) PID 코드 추출
            val pidHex = compact.substring(hexOffset, hexOffset + 2)
            val pid = PIDs.fromCode("01$pidHex") ?: break

            // 3) 해당 PID 데이터 길이 결정 (bytes)
            val dataLen =
                when (pid) {
                    PIDs.RPM,
                    PIDs.MAF,
                    PIDs.BATT,
                    PIDs.CATALYST_TEMP_BANK1,
                    PIDs.COMMANDED_EQUIVALENCE_RATIO,
                    -> 2

                    else -> 1
                }

            // 4) f-segment: mode+pid+데이터 부분만 잘라서 파서에 전달
            val segStart = hexOffset - 2
            val segEnd = hexOffset + 2 + dataLen * 2
            val segment = compact.substring(segStart, segEnd)

            // 5) 파싱 및 맵에 저장
            val value = Decoders.parsers[pid]!!.invoke(segment)
            result[pid] = value

            // 6) offset 이동 (pid(1byte) + 데이터)
            hexOffset += 2 + dataLen * 2
        }
        return result
    }
}
