package com.devidea.aicar.storage.room.dtc

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DtcInfoDao {
    @Query("SELECT * FROM dtc_info WHERE code = :code LIMIT 1")
    suspend fun getInfoByCode(code: String): DtcInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<DtcInfoEntity>)
}