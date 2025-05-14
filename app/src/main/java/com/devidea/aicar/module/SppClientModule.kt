package com.devidea.aicar.module

import com.devidea.aicar.service.SppClient
import com.devidea.aicar.service.SppClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// SppClientModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class SppClientModule {
    /**
     * SppClientImpl 은 @Inject 생성자를 가지고 있으므로,
     * Hilt가 알아서 Context, DataStoreRepository, RecordUseCase, DrivingRepository를
     * 주입해 줍니다.
     */
    @Binds
    @Singleton
    abstract fun bindSppClient(
        impl: SppClientImpl
    ): SppClient
}