package com.devidea.chevy.module

import android.content.Context
import androidx.room.Room
import com.devidea.chevy.storage.room.AppDatabase
import com.devidea.chevy.storage.room.document.DocumentDao
import com.devidea.chevy.storage.room.document.DocumentRepository
import com.devidea.chevy.storage.room.drive.DrivingDao
import com.devidea.chevy.storage.room.drive.DrivingRepository
import com.devidea.chevy.storage.room.drive.DrivingRepositoryImpl
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
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(dao: DocumentDao): DocumentRepository {
        return DocumentRepository(dao)
    }

    @Provides
    @Singleton
    fun provideDrivingDao(database: AppDatabase): DrivingDao {
        return database.driveDAO()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RepositoryModule {

        @Binds
        @Singleton
        abstract fun bindDrivingRepository(
            impl: DrivingRepositoryImpl
        ): DrivingRepository
    }
}