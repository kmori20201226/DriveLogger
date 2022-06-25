package com.kmoriproj.drivelogger.repositories

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.common.Polyline
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val context: Context,
    private val sharedLocationManager: SharedLocationManager
) {
    /**
     * Observable flow for location updates
     */
    val locationFlow = sharedLocationManager.locationFlow

    val lastLocation: Task<Location> get() = sharedLocationManager.lastLocation

    val gpsTracker: GPSTracker get() = sharedLocationManager.gps

    private val _pathPoints =  MutableLiveData<Polyline>(mutableListOf())

    val pathPoints : LiveData<Polyline> = _pathPoints

    private val _distanceFromStartKm = MutableLiveData<Float>()

    val distanceFromStartKm: LiveData<Float> = _distanceFromStartKm

    private var locationFlowJob: Job? = null

    fun startTrackLocation() {
        locationFlowJob =
            sharedLocationManager.locationFlow
                .onEach {
                    _pathPoints.value?.add(LatLng(it.latitude, it.longitude))
                    _pathPoints.postValue(_pathPoints.value)
                    _distanceFromStartKm.postValue(gpsTracker.distanceFromStartKm)
                }
                .launchIn((context.applicationContext as BaseApplication).applicationScope)
    }

    fun endTrackLocation() {
        locationFlowJob?.cancel()
        _pathPoints.value = mutableListOf()
    }

}