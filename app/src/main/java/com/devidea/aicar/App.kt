package com.devidea.aicar

import android.app.Application
import android.util.Log
import com.devidea.aicar.drive.usecase.RecordUseCase
import com.kakao.vectormap.KakaoMapSdk
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.KNSDKDelegate
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    companion object {
        const val CHANNEL_ID = "LeBluetoothServiceChannel"
        lateinit var instance: App
    }

    @Inject
    lateinit var recordUseCase: RecordUseCase

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("MyApp", "RecordUseCase initialized: $recordUseCase")
    }
}