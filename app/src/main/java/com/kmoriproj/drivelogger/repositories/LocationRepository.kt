package com.kmoriproj.drivelogger.repositories

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.*
import com.kmoriproj.drivelogger.common.Constants.Companion.KEY_START_MOVING_SPEED
import com.kmoriproj.drivelogger.common.Constants.Companion.KEY_STAY_TIME_THRESHOLD
import com.kmoriproj.drivelogger.db.Spot
import com.kmoriproj.drivelogger.db.SpotDao
import com.kmoriproj.drivelogger.services.SharedPreferenceUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val context: Context,
    private val sharedLocationManager: SharedLocationManager,
    private val spotDao: SpotDao
) : SharedPreferences.OnSharedPreferenceChangeListener  {
    /**
     * Observable flow for location updates
     */
    val locationFlow = sharedLocationManager.locationFlow

    fun getLastLocation(): Task<Location> = sharedLocationManager.getLastLocation()

    val gpsTracker: GPSTracker get() = sharedLocationManager.gps

    private val _spots = mutableListOf<Spot>()
    private val _stillSpots = MutableLiveData<List<Spot>>(mutableListOf())
    val stillSpots: LiveData<List<Spot>> = _stillSpots

    inner class MovementSensor(val initialLocation: LocationSnapshot) {

        private var pivot: LatLng = initialLocation.latlng
        private var last = initialLocation
        private val accum = mutableListOf<LocationSnapshot>()

        private val center: LatLng
            get() {
                var lat = initialLocation.latlng.latitude
                var lon = initialLocation.latlng.longitude
                accum.forEach {
                    lat += it.latlng.latitude
                    lon += it.latlng.longitude
                }
                val n = accum.size + 1
                return LatLng(lat / n, lon / n)
            }

        private val radius: Float
            get() {
                val c = center
                var maxDistance = initialLocation.latlng.distanceTo(c)
                accum.forEach {
                    val d = it.latlng.distanceTo(c)
                    if (d > maxDistance) {
                        maxDistance = d
                    }
                }
                return maxDistance
            }

        fun withinRange(curr: LocationSnapshot): Boolean {
            val startMovingSpeed = sharedPreferences.getInt(KEY_START_MOVING_SPEED, 20).toFloat()
            val stayTimeThreshold = sharedPreferences.getInt(KEY_STAY_TIME_THRESHOLD, 180)

            val d = curr.latlng.distanceTo(last.latlng)
            val t = (curr.time - last.time) / 1000
            val speed = (d / 1000.0) / (t / 3600.0)
            if (speed < startMovingSpeed) {
                accum.add(curr)
                return true
            }
            //if (curr.latlng.distanceTo(pivot) <= radiusThreshold) {
            //    return true
            //}
            val stayTime = (curr.time - initialLocation.time) / 1000
            if (stayTime >= stayTimeThreshold) {
                val x = this@LocationRepository._stillSpots.value
                this@LocationRepository._spots.add(
                    Spot(
                        tripId=this@LocationRepository.sharedLocationManager.tripId!!,
                        stayTime=stayTime,
                        point=center,
                        radius=radius
                    )
                )
                this@LocationRepository._stillSpots.postValue(_spots)
            }
            return false
        }
    }

    private var stay1: MovementSensor? = null

    private fun keepTrackStill(curr: LocationSnapshot) {
        if (stay1 == null) {
            stay1 = MovementSensor(curr)
        } else {
            if (stay1!!.withinRange(curr) == false) {
                stay1 = null
            }
        }
    }

    private val _pathPoints =  MutableLiveData<Polyline>(mutableListOf())

    val pathPoints : LiveData<Polyline> = _pathPoints

    private val _distanceFromStartKm = MutableLiveData<Float>()

    val distanceFromStartKm: LiveData<Float> = _distanceFromStartKm

    private var locationFlowJob: Job? = null

    fun startTrackLocation() {
        locationFlowJob =
            sharedLocationManager.locationFlow
                .onEach {
                    _pathPoints.value?.add(RichPoint.makeFrom(it, pathPoints.value!!))
                    _pathPoints.postValue(_pathPoints.value)
                    keepTrackStill(it)
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
            coroutinesScope.launch {
                startTrip()
            }
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
        _distanceFromStartKm.value = 0.0f
        _timeRunInSeconds.value = 0
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
        coroutinesScope.launch {
            gpsTracker.flush()
        }
    }

    private val job = Job()
    private val coroutinesScope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)

    fun saveTrip() {
        coroutinesScope.launch {
            gpsTracker.saveTrip()
            _spots.forEach {
                spotDao.insertSpot(it)
            }
        }
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context!!);

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