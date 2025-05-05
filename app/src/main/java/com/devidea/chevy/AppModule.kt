package com.devidea.chevy

import android.content.Context
import androidx.room.Room.databaseBuilder
import com.devidea.chevy.k10s.obd.model.CarEventModel
import com.devidea.chevy.k10s.obd.model.ControlModule
import com.devidea.chevy.k10s.obd.model.TPMSModule
import com.devidea.chevy.k10s.BleServiceManager
import com.devidea.chevy.service.SppClient
import com.devidea.chevy.service.SppClientImpl
import com.devidea.chevy.storage.AppDatabase
import com.devidea.chevy.storage.DocumentDao
import com.devidea.chevy.storage.DocumentRepository
import com.devidea.chevy.ui.navi.KNavi
import dagger.Binds
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
    fun provideSppServiceManager(@ApplicationContext context: Context): SppClientImpl {
        return SppClientImpl(context)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class SppModule {
        @Binds
        @Singleton
        abstract fun bindSppClient(
            impl: SppClientImpl
        ): SppClient
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return databaseBuilder<AppDatabase>(
            context,
            "app_database"
        )
            .build()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(dao: DocumentDao): DocumentRepository {
        return DocumentRepository(dao)
    }
}