package com.devidea.aicar

import android.app.Application
import android.util.Log
import com.devidea.aicar.drive.RecordUseCase
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
        KNSDK.apply {
            // 콘텍스트 등록 및 DB, 파일 등의 저장 경로 설정
            install(instance, "$filesDir/files")
            delegate = object: KNSDKDelegate {
                override fun knsdkFoundUnfinishedTrip(aTrip: KNTrip, aPriority: KNRoutePriority, aAvoidOptions: Int) {
                    Logger.d{"knsdkFoundUnfinishedTrip"}
                }
                override fun knsdkNeedsLocationAuthorization() {
                    Logger.d{"knsdkNeedsLocationAuthorization"}
                }
            }
        }

        KakaoMapSdk.init(this, "e31e85ed66b03658041340618628e93f");
    }
}