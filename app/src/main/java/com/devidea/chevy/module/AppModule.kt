package com.devidea.chevy.module

import android.content.Context
import androidx.room.Room
import com.devidea.chevy.LocationProvider
import com.devidea.chevy.service.SppClient
import com.devidea.chevy.service.SppClientImpl
import com.devidea.chevy.storage.room.AppDatabase
import com.devidea.chevy.storage.room.document.DocumentDao
import com.devidea.chevy.storage.room.document.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

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

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ApplicationScope

    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}