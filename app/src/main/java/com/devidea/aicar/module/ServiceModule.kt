package com.devidea.aicar.module

import android.content.Context
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.service.SppClientImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServiceModule {
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
}