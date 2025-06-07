package com.devidea.aicar.module

import android.content.Context
import androidx.room.Room
import com.devidea.aicar.storage.room.AppDatabase
import com.devidea.aicar.storage.room.drive.DrivingDao
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.drive.DrivingRepositoryImpl
import com.devidea.aicar.storage.room.dtc.DtcInfoDao
import com.devidea.aicar.storage.room.dtc.DtcInfoRepository
import com.devidea.aicar.storage.room.dtc.DtcInfoRepositoryImpl
import com.devidea.aicar.storage.room.notification.NotificationDao
import com.devidea.aicar.storage.room.notification.NotificationRepository
import com.devidea.aicar.storage.room.notification.NotificationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(
            context,
            "app_database"
        )
            .build()
    }

    @Provides
    @Singleton
    fun provideDrivingDao(database: AppDatabase): DrivingDao = database.driveDAO()

    @Provides
    @Singleton
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    @Singleton
    fun provideDtcInfoRepository(database: AppDatabase): DtcInfoDao = database.dtcInfoDao()

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RepositoryModule {

        @Binds
        @Singleton
        abstract fun bindDrivingRepository(
            impl: DrivingRepositoryImpl
        ): DrivingRepository

        @Binds
        @Singleton
        abstract fun bindNotificationRepository(
            impl: NotificationRepositoryImpl
        ): NotificationRepository

        @Binds
        @Singleton
        abstract fun bindDtcRepository(
            impl: DtcInfoRepositoryImpl
        ): DtcInfoRepository
    }
}