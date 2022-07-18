package com.kmoriproj.drivelogger.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Trip::class, Trajectory::class, Spot::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DriveLoggerDatabase : RoomDatabase() {

    abstract fun getTrajectoryDao(): TrajectoryDao

    abstract fun getTripDao(): TripDao

    abstract fun getSpotDao(): SpotDao
}
