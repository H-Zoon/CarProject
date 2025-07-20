package com.devidea.aicar.drive

object ExtendedPidUtils {
    private const val MODE = "22"
    internal const val MAX_PIDS = 1

    /** 주어진 확장 PID 리스트로 “22 XX XX YY YY ZZ ZZ” 형태 명령어 생성 */
    fun buildCommand(pids: List<PIDs>): String {
        require(pids.all { it.code.startsWith(MODE) }) {
            "Mode22 전용 PID만 지원합니다."
        }
        require(pids.size in 1..MAX_PIDS) {
            "확장 PID는 한 번에 최대 $MAX_PIDS 개만."
        }

        // PIDs.code == "22XXXX" 에서 앞 두글자 잘라낸 "XXXX"들
        val bodies = pids.map { it.code.substring(2) }
        val raw = (MODE + bodies.joinToString("")).chunked(2).joinToString(" ")
        return raw // ex: "22 19 9A 11 5C"
    }

    /** 확장 응답 파싱: 전체 문자열에서 Mode22 응답만 떼어내고, 2바이트 식별자+1바이트 데이터로 분리 */
    fun parseResponse(resp: String): Map<PIDs, Number> {
        val compact = resp.replace("\\s".toRegex(), "").uppercase()
        require(compact.startsWith("62")) { "Mode22 응답 아님: ${compact.take(2)}" }

        val result = mutableMapOf<PIDs, Number>()
        var offset = 2 // "62" 이후 바로
        while (offset + 6 <= compact.length) {
            // 2바이트 PID 식별자
            val pidKey = compact.substring(offset, offset + 4) // ex: "199A"
            val pid = PIDs.fromCode(MODE + pidKey) ?: break

            // 1바이트(2hex) 데이터
            val dataHex = compact.substring(offset + 4, offset + 6)
            val frame = MODE + pidKey + dataHex // "62 19 9A 0F" 형태
            val value = Decoders.parsers[pid]!!.invoke(frame)

            result[pid] = value
            offset += 6 // PID(4chars) + 데이터(2chars)
        }
        return result
    }
}
