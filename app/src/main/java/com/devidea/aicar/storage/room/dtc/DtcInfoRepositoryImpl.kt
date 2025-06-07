package com.devidea.aicar.storage.room.dtc

import javax.inject.Inject

class DtcInfoRepositoryImpl @Inject constructor(private val dao: DtcInfoDao) : DtcInfoRepository {
    override suspend fun getDtcInfo(code: String): DtcInfoEntity? {
        return dao.getInfoByCode(code)
    }

    override suspend fun setDtcInfo(list: List<DtcInfoEntity>) {
        dao.insertAll(list)
    }
}