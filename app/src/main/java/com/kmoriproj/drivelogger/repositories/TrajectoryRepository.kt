package com.kmoriproj.drivelogger.repositories

import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.db.TrajectoryDao
import javax.inject.Inject

class TrajectoryRepository @Inject constructor(
    val trajectoryDao: TrajectoryDao
) {
    suspend fun insertTrajectory(traj: Trajectory) = trajectoryDao.insertTrajectory(traj)
}