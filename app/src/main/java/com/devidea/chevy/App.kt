package com.devidea.chevy

import android.app.Application
import android.content.Context

class App : Application() {

    init{
        instance = this
    }

    companion object {
        lateinit var instance: App
        fun ApplicationContext() : Context {
            return instance.applicationContext
        }
    }

}