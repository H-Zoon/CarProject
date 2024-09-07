package com.devidea.chevy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.v2.common.BuildConfig.VERSION_NAME
import com.kakao.vectormap.KakaoMapSdk
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.KNSDKDelegate
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    init{
        instance = this
    }

    companion object {
        const val CHANNEL_ID = "LeBluetoothServiceChannel"
        lateinit var instance: App
        fun ApplicationContext() : Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "LeBluetoothService Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)

        KNSDK.apply {
            // 콘텍스트 등록 및 DB, 파일 등의 저장 경로 설정
            install(instance, "$filesDir/files")
            delegate = object: KNSDKDelegate {
                override fun knsdkFoundUnfinishedTrip(aTrip: KNTrip, aPriority: KNRoutePriority, aAvoidOptions: Int) {
                    Log.d("APP", "knsdkFoundUnfinishedTrip")
                }
                override fun knsdkNeedsLocationAuthorization() {
                    Log.d("APP", "knsdkNeedsLocationAuthorization")
                }
            }
        }

        KakaoMapSdk.init(this, "e31e85ed66b03658041340618628e93f");
    }
}