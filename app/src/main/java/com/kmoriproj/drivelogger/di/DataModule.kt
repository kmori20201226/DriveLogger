package com.kmoriproj.drivelogger.di

import android.content.Context
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.repositories.LocationRepository
import com.kmoriproj.drivelogger.repositories.SharedLocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for DI on the repository and shared location manager
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSharedLocationManager(
        @ApplicationContext context: Context,
        gps: GPSTracker,
    ): SharedLocationManager =
        SharedLocationManager(context, gps)

}
