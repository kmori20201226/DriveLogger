package com.kmoriproj.drivelogger.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(Trip: Trip) : Long

    @Delete
    suspend fun deleteTrip(Trip: Trip)

    @Query("SELECT * FROM trip ORDER BY startTime DESC")
    fun getAllTripsByStartTime(): LiveData<List<Trip>>

}