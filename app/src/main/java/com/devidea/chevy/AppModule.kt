package com.devidea.chevy

import android.app.Application
import android.content.Context
import com.devidea.chevy.datas.obd.model.CarEventModel
import com.devidea.chevy.datas.obd.model.ControlModule
import com.devidea.chevy.datas.obd.model.TPMSModule
import com.devidea.chevy.service.BleService
import com.devidea.chevy.service.BleServiceManager
import com.devidea.chevy.ui.screen.navi.KNavi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return LocationProvider(context)
    }

    @Provides
    @Singleton
    fun provideBleServiceManager(@ApplicationContext context: Context): BleServiceManager {
        return BleServiceManager(context)
    }

    @Provides
    @Singleton
    fun provideCarEventModel(
        bleServiceManager: BleServiceManager
    ): CarEventModel {
        return CarEventModel(bleServiceManager)
    }

    @Provides
    @Singleton
    fun provideControlModule(
        bleServiceManager: BleServiceManager
    ): ControlModule {
        return ControlModule(bleServiceManager)
    }

    @Provides
    @Singleton
    fun provideTPMSModule(
        bleServiceManager: BleServiceManager
    ): TPMSModule {
        return TPMSModule(bleServiceManager)
    }

    @Provides
    @Singleton
    fun provideKNavi(
        bleServiceManager: BleServiceManager
    ): KNavi {
        return KNavi(bleServiceManager)
    }
}