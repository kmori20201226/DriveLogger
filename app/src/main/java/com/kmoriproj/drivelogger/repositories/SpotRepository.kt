package com.kmoriproj.drivelogger.repositories

import com.kmoriproj.drivelogger.db.Spot
import com.kmoriproj.drivelogger.db.SpotDao
import javax.inject.Inject

class SpotRepository @Inject constructor(
    val spotDao: SpotDao
) {
    suspend fun insertSpot(spot: Spot) = spotDao.insertSpot(spot)

    suspend fun deleteSpot(spot: Spot) = spotDao.deleteSpot(spot)

    fun getSpot(spotId: Long) = spotDao.getSpot(spotId)

    fun getSpotsByTripId(tripId: Long) = spotDao.getSpotsByTripId(tripId)

}