package com.kmoriproj.drivelogger.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SpotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: Spot) : Long

    @Delete
    suspend fun deleteSpot(spot: Spot)

    @Query("SELECT * FROM spot WHERE id = :id")
    fun getSpot(id: Long) : LiveData<Spot>

    @Query("SELECT * FROM spot WHERE tripid = :tripId")
    fun getSpotsByTripId(tripId: Long) : LiveData<List<Spot>>

}