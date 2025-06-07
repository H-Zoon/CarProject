package com.devidea.aicar.storage.room.dtc

interface DtcInfoRepository {
    suspend fun getDtcInfo(code: String): DtcInfoEntity?

    suspend fun setDtcInfo(list: List<DtcInfoEntity>)
}