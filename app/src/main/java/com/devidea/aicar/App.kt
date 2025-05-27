package com.devidea.aicar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp
class App : Application() {
    companion object {
        const val ACTION_AUTO_CONNECT = "com.aicar.ACTION_AUTO_CONNECT"
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        //Log.d("MyApp", "RecordUseCase initialized: $recordUseCase")
    }
}