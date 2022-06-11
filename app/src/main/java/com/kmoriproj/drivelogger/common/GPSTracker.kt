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

    private var writer: CsvFileWriter? = null
    private val trajBuf = mutableListOf<TrajPoint>()
    private val trajBufSpill = 100
    val job = Job()
    val coroutinesScope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)
    var lastPos: LatLng? = null
    //val locationList = mutableListOf<Location>()
    val distanceFromStartKm
        get() = (currentTrip?.distanceFromStart ?: 0.0f) / 1000.0f
    fun reset() {
        writer?.close()
        writer = null
        trajBuf.clear()
        lastPos = null
        //locationList.clear()
    }
    private fun openDumpfile() {
        val dir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS).absolutePath + "/DriveLogger")
        dir.mkdirs()
        val now = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val file = File(dir, "location-dump-%s.csv".format(now))
        @OptIn(KotlinCsvExperimental::class)
        writer = csvWriter().openAndGetRawWriter(file)
        writer?.writeRow(header())
    }
    private fun header(): List<String> {
        return listOf(
            "time",
            "hasAccuracy",
            "accuracy",
            "hasAltitude",
            "altitude",
            "hasBearing",
            "bearing",
            "hasBearingAccuracy",
            "bearingAccuracyDegrees",
            "hasElapsedRealtimeUncertaintyNanos",
            "elapsedRealtimeUncertaintyNanos",
            "elapsedRealtimeNanos",
            "elapsedRealtimeUncertaintyNanos",
            "latitude",
            "longitude",
            "provider",
            "hasSpeed",
            "speed",
            "hasSpeedAccuracy",
            "speedAccuracyMetersPerSecond",
            "hasVerticalAccuracy",
            "verticalAccuracyMeters",
        )
    }
    fun startTrip() {
        currentTrip = Trip()
        liveCurrentTrip.value = currentTrip
        coroutinesScope.launch {
            val id = tripRepository.insertTrip(currentTrip!!)
            currentTrip?.id = id
            Timber.d("Trip added " + id.toString())
        }
    }
    fun finishTrip() {
        coroutinesScope.launch {
            tripRepository.insertTrip(currentTrip!!)
            Timber.d("Trip updated")
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
        val row = listOf(
            loc.time,
            loc.hasAccuracy(),
            loc.accuracy,
            loc.hasAltitude(),
            loc.altitude,
            loc.hasBearing(),
            loc.bearing,
            loc.hasBearingAccuracy(),
            loc.bearingAccuracyDegrees,
            loc.hasElapsedRealtimeUncertaintyNanos(),
            loc.elapsedRealtimeUncertaintyNanos,
            loc.elapsedRealtimeNanos,
            loc.elapsedRealtimeUncertaintyNanos,
            loc.latitude,
            loc.longitude,
            loc.provider,
            loc.hasSpeed(),
            loc.speed,
            loc.hasSpeedAccuracy(),
            loc.speedAccuracyMetersPerSecond,
            loc.hasVerticalAccuracy(),
            loc.verticalAccuracyMeters,
        )
        if (writer == null) {
            openDumpfile()
        }
        if (currentTrip?.startTime == 0L) {
            currentTrip?.startTime = loc.time
        }
        currentTrip?.endTime = loc.time
        trajBuf.add(TrajPoint.fromLocation(loc))
        if (trajBuf.size >= trajBufSpill) {
            flush()
        }
        writer?.writeRow(row)
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
                liveCurrentTrip.value = currentTrip
                return true
            }
        }
        return false
    }
    fun close() {
        writer?.close()
        writer = null
    }
    protected fun finalize() {
        close()
    }
}