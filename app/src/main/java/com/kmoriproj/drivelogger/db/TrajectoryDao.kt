package com.kmoriproj.drivelogger.db

import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrajectoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTrajectory(trajectory: Trajectory)

    @Delete
    abstract suspend fun deleteTrajectories(trajectories: Trajectory)

    @Query("SELECT * FROM trajectories WHERE tripId = :tripId ORDER BY startTime ASC")
    abstract fun getTrajectoriesOfTrip(tripId: Long): Flow<List<Trajectory>>

    @Query("SELECT * FROM trajectories WHERE tripId = :tripId ORDER BY startTime ASC")
    abstract suspend fun getBlockedTrajectoriesOfTrip(tripId: Long): List<Trajectory>

    @Query("SELECT * FROM trajectories ORDER BY tripId ASC, startTime ASC")
    abstract fun getAllTrajectories(): Flow<List<Trajectory>>
}