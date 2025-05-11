package com.devidea.aicar

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.KNSDKDelegate
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    companion object {
        const val CHANNEL_ID = "LeBluetoothServiceChannel"
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

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