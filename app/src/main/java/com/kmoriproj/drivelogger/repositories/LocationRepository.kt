package com.kmoriproj.drivelogger.repositories

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants
import com.kmoriproj.drivelogger.common.GPSTracker
import com.kmoriproj.drivelogger.common.Polyline
import com.kmoriproj.drivelogger.services.SharedPreferenceUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val context: Context,
    private val sharedLocationManager: SharedLocationManager
) : SharedPreferences.OnSharedPreferenceChangeListener  {
    /**
     * Observable flow for location updates
     */
    val locationFlow = sharedLocationManager.locationFlow

    fun getLastLocation(): Task<Location> = sharedLocationManager.getLastLocation()

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
                    _pathPoints.value?.add(it.latlng)
                    _pathPoints.postValue(_pathPoints.value)
                    _distanceFromStartKm.postValue(gpsTracker.distanceFromStartKm)
                }
                .launchIn((context.applicationContext as BaseApplication).applicationScope)
    }

    fun endTrackLocation() {
        locationFlowJob?.cancel()
        _pathPoints.value = mutableListOf()
    }

    private var timerJob: Job? = null

    private var startTripTime: Long? = null

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _isTravelling = MutableLiveData<Boolean>(false)
    val isTravelling: LiveData<Boolean> = _isTravelling

    private val _timeRunInSeconds = MutableLiveData<Long>()
    val timeRunInSeconds: LiveData<Long> = _timeRunInSeconds

    fun startTracking() {
        Log.d("OvO", "ViewModel::startTracking")
        if (_isTravelling.value != true) {
            _isTracking.value = true
            _isTravelling.value = true
            startTrip()
        }
        startTimer()
    }

    private fun startTrip() {
        Log.d("OvO", "ViewModel::startTrip")
        startTripTime = System.currentTimeMillis()
        startTrackLocation()
    }

    fun endTrip() {
        Log.d("OvO", "ViewModel::endTrip")
        endTrackLocation()
        _isTravelling.value = false
    }

    fun endTracking() {
        timerJob?.cancel()
    }

    fun pauseTracking() {
        _isTracking.value = false
    }

    fun resumeTracking() {
        _isTracking.value = true
    }
    /**
     * Starts the timer for the tracking.
     */
    private fun startTimer() {
        var lastLapTime = 0L
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // time difference between now and time started
                val lapTime = (System.currentTimeMillis() - startTripTime!!) / 1000
                if (lastLapTime != lapTime) {
                    lastLapTime = lapTime
                    _timeRunInSeconds.postValue(lapTime)
                }
                delay(Constants.TIMER_UPDATE_INTERVAL)
            }
        }
    }

    fun flush() {
        gpsTracker.flush()
    }

    fun saveTrip() {
        gpsTracker.saveTrip()
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(context.getString(
        R.string.preference_file_key), Context.MODE_PRIVATE)

    private val isForegroundEnabled
        get() = sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

    fun initialize() {
        sharedPreferences.registerOnSharedPreferenceChangeListener (this)
    }

    fun finalize() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            _isTracking.postValue(isForegroundEnabled)
        }
    }

}