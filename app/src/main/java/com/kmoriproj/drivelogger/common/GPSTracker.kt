package com.kmoriproj.drivelogger.common

import android.location.Location
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.maps.model.LatLng
import com.kmoriproj.drivelogger.db.*
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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
    val job = Job()
    val coroutinesScope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)
    var lastPos: LatLng? = null
    //val locationList = mutableListOf<Location>()
    val distanceFromStartKm
        get() = (currentTrip?.distanceFromStart ?: 0.0f) / 1000.0f
    fun reset() {
        trajBuf.clear()
        lastPos = null
        //locationList.clear()
    }
    fun startTrip() {
        currentTrip = Trip()
        liveCurrentTrip.postValue(currentTrip)
        coroutinesScope.launch {
            val id = tripRepository.insertTrip(currentTrip!!)
            currentTrip?.id = id
            Timber.d("Trip added " + id.toString())
        }
    }
    fun saveTrip() {
        coroutinesScope.launch {
            flush()
            tripRepository.insertTrip(currentTrip!!)
            Timber.d("Trip save")
        }
    }
    fun flush() {
        if (trajBuf.size > 0) {
            val trajRec = Trajectory(
                tripId = currentTrip?.id,
                trajPoints = TrajPointList(trajBuf.toList())
            )
            trajBuf.clear()
            coroutinesScope.launch {
                trajectoryRepository.insertTrajectory(trajRec)
                Timber.d("Record added")
            }
            currentTrip?.let {
                it.numDataPoints = it.numDataPoints + trajRec.size
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun addLocation(loc: Location) : Boolean {
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
            //locationList.add(loc)
            return true
        } else {
            val result = FloatArray(1)
            Location.distanceBetween(
                pos.latitude, pos.longitude,
                lastPos!!.latitude, lastPos!!.longitude,
                result)
            val distanceInMeter = result[0]
            if (distanceInMeter >= 2.0) {
                lastPos = pos
                //locationList.add(loc)
                currentTrip?.let {
                    it.distanceFromStart += distanceInMeter
                }
                liveCurrentTrip.postValue(currentTrip)
                return true
            }
        }
        return false
    }
}