package com.devidea.aicar.module

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.devidea.aicar.LocationProvider
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
    fun provideApplicationContext(
        @ApplicationContext context: Context,
    ): Context = context

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context,
    ): LocationProvider = LocationProvider(context)

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ApplicationScope

    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideBluetoothAdapter(
        @ApplicationContext context: Context,
    ): BluetoothAdapter =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 에서는 BluetoothManager 로부터 얻는 것이 권장됩니다.
            val manager =
                context.getSystemService(BluetoothManager::class.java)
                    ?: throw IllegalStateException("BluetoothManager not available")
            manager.adapter
        } else {
            // Android 12 미만에서는 getDefaultAdapter() 사용
            BluetoothAdapter.getDefaultAdapter()
                ?: throw IllegalStateException("BluetoothAdapter not available")
        }
}
