package com.kmoriproj.drivelogger.db

import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class TrajectoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTrajectory(trajectory: Trajectory)

    @Delete
    abstract suspend fun deleteTrajectories(trajectories: Trajectory)

    @Query("SELECT * FROM trajectories WHERE tripId = :tripId ORDER BY startTime ASC")
    abstract fun getTrajectoriesOfTrip(tripId: Int): LiveData<List<Trajectory>>
}