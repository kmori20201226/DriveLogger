package com.kmoriproj.drivelogger

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EndOfTripViewModel @Inject constructor(
    val gpsTracker: GPSTracker
) : ViewModel() {
    val liveCurrentTrip: LiveData<Trip> get() = gpsTracker.liveCurrentTrip
    fun finishTrip() {
        gpsTracker.finishTrip()
    }
}
