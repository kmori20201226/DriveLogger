package com.kmoriproj.drivelogger.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.kmoriproj.drivelogger.db.TrajPoint
import com.kmoriproj.drivelogger.db.TrajPointList
import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.db.TrajectoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TrajectoryRepository @Inject constructor(
    val trajectoryDao: TrajectoryDao
) {
    suspend fun insertTrajectory(traj: Trajectory) = trajectoryDao.insertTrajectory(traj)

    fun getTrajectoriesOfTrip(tripId: Long) = trajectoryDao.getTrajectoriesOfTrip(tripId)

    suspend fun getBlockedTrajectoriesOfTrip(tripId: Long) = trajectoryDao.getBlockedTrajectoriesOfTrip(tripId)

    fun getAllTrajectories() = trajectoryDao.getAllTrajectories()
}