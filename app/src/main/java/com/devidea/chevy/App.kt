package com.devidea.chevy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
    }
}