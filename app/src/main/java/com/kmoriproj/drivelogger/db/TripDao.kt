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
    fun getAllTripsByDate(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip ORDER BY startTime % (24*60*60*1000) ASC")
    fun getAllTripsByStartTime(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip ORDER BY distanceFromStart DESC")
    fun getAllTripsByFarest(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip ORDER BY distanceFromStart ASC")
    fun getAllTripsByNearest(): LiveData<List<Trip>>

}