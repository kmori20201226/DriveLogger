package com.kmoriproj.drivelogger.repositories

import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.db.TrajectoryDao
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.db.TripDao
import javax.inject.Inject

class TripRepository @Inject
constructor(
    val tripDao: TripDao
) {
    suspend fun insertTrip(trip: Trip) = tripDao.insertTrip(trip)
}
