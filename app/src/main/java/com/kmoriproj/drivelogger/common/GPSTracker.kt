package com.kmoriproj.drivelogger.common

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.kmoriproj.drivelogger.db.*
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class GPSTracker @Inject constructor(
    val trajectoryRepository: TrajectoryRepository,
    val tripRepository: TripRepository
) {
    var currentTrip: Trip? = null
    val liveCurrentTrip: MutableLiveData<Trip> = MutableLiveData<Trip>(currentTrip)
    //val liveCurrentTrip: LiveData<Trip> = _liveCurrentTrip

    private val trajBuf = mutableListOf<TrajPoint>()
    private val trajBufSpill = 100
    var lastPos: LatLng? = null
    var lastTick: Long? = null
    //val locationList = mutableListOf<Location>()
    val distanceFromStartKm
        get() = (currentTrip?.distanceFromStart ?: 0.0f) / 1000.0f

    fun reset() {
        trajBuf.clear()
        lastPos = null
        lastTick = null
    }

    suspend fun startTrip() {
        currentTrip = Trip()
        liveCurrentTrip.postValue(currentTrip)
        val id = tripRepository.insertTrip(currentTrip!!)
        currentTrip?.id = id
        Timber.d("Trip added " + id.toString())
    }

    suspend fun saveTrip() {
        flush()
        tripRepository.insertTrip(currentTrip!!)
        Timber.d("Trip save")
    }

    suspend fun flush() {
        if (trajBuf.size > 0) {
            val trajRec = Trajectory(
                tripId = currentTrip?.id,
                trajPoints = TrajPointList(trajBuf.toList())
            )
            trajBuf.clear()
            trajectoryRepository.insertTrajectory(trajRec)
            Timber.d("Record added")
            currentTrip?.let {
                it.numDataPoints = it.numDataPoints + trajRec.size
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun addLocation(loc: Location) : LocationSnapshot? {
        val pos = LatLng(loc.latitude, loc.longitude)
        if (currentTrip?.startTime == 0L) {
            currentTrip?.startTime = loc.time
        }
        currentTrip?.endTime = loc.time
        trajBuf.add(TrajPoint.fromLocation(loc))
        if (trajBuf.size >= trajBufSpill) {
            flush()
        }
        if (lastPos == null) {
            lastPos = pos
            lastTick = loc.time
            return LocationSnapshot(
                time = loc.time,
                speed = loc.speed,
                latlng = pos,
                travelDistance = 0.0f,
                travelTime = 0,
                distanceInMeter = 0.0f,
                timeSpan = 0
            )
        } else {
            val distanceInMeter = pos.distanceTo(lastPos!!)
            if (distanceInMeter >= 2.0) {
                currentTrip?.let {
                    it.distanceFromStart += distanceInMeter
                }
                liveCurrentTrip.postValue(currentTrip)
                return LocationSnapshot(
                    time = loc.time,
                    speed = loc.speed,
                    latlng = pos,
                    travelDistance = currentTrip?.distanceFromStart ?: 0.0f,
                    travelTime = loc.time - (currentTrip?.startTime ?: loc.time),
                    distanceInMeter = distanceInMeter,
                    timeSpan = loc.time - lastTick!!
                ).also {
                    lastPos = pos
                    lastTick = loc.time
                }
            }
        }
        Log.d("OvO", "ADD Location Skipped")
        return null
    }
}