package com.devidea.aicar

import android.app.Application
import android.util.Log
import com.devidea.aicar.drive.usecase.RecordUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    companion object {
        const val NOTIFICATION_TITLE = "AICAR"
        const val NOTIFICATION_BODY = "AICAR 서비스가 시작되었습니다."
        const val FOREGROUND_CHANNEL_ID = "AICAR"
        const val FOREGROUND_CHANNEL_NAME = "AICAR_bluetooth"

        const val ACTION_AUTO_CONNECT = "com.aicar.ACTION_AUTO_CONNECT"
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        //Log.d("MyApp", "RecordUseCase initialized: $recordUseCase")
    }
}