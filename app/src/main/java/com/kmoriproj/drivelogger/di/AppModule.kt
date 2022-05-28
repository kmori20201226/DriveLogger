package com.kmoriproj.drivelogger.di

import android.app.Application
import androidx.room.Room
import com.kmoriproj.drivelogger.common.Constants.Companion.DATABASE_NAME
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.db.DriveLoggerDatabase
import com.kmoriproj.drivelogger.db.TrajectoryDao
import com.kmoriproj.drivelogger.db.TripDao
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule, provides application wide singletons
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideAppDb(app: Application): DriveLoggerDatabase {
        return Room.databaseBuilder(app, DriveLoggerDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideTrajectoryDao(db: DriveLoggerDatabase): TrajectoryDao {
        return db.getTrajectoryDao()
    }

    @Singleton
    @Provides
    fun provideTripDao(db: DriveLoggerDatabase): TripDao {
        return db.getTripDao()
    }

    @Singleton
    @Provides
    fun provideGPSTracker(
        trajectoryRepository: TrajectoryRepository,
        tripRepository: TripRepository
    ) = GPSTracker(trajectoryRepository, tripRepository)
}