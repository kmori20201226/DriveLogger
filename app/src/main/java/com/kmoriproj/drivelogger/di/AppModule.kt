package com.kmoriproj.drivelogger.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.kmoriproj.drivelogger.common.Constants.Companion.DATABASE_NAME
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.db.DriveLoggerDatabase
import com.kmoriproj.drivelogger.db.TrajectoryDao
import com.kmoriproj.drivelogger.db.TripDao
import com.kmoriproj.drivelogger.repositories.LocationRepository
import com.kmoriproj.drivelogger.repositories.SharedLocationManager
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import com.kmoriproj.drivelogger.ui.viewmodels.DrivingViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Singleton
    @Provides
    fun providesDrivingViewModel(
        @ApplicationContext context: Context,
        repository: LocationRepository
    ) = DrivingViewModel(context, repository)

    @Singleton
    @Provides
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        sharedLocationManager: SharedLocationManager,
    ) = LocationRepository(context, sharedLocationManager)
}
