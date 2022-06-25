package com.kmoriproj.drivelogger.ui

import android.content.*
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants
import com.kmoriproj.drivelogger.common.Polyline
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.LocationRepository
import com.kmoriproj.drivelogger.services.ForegroundOnlyLocationService
import com.kmoriproj.drivelogger.services.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DrivingViewModel @Inject constructor(
        val context: Context,
        private val repository: LocationRepository) : ViewModel(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var timerJob: Job? = null

    private var startTripTime: Long? = null

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _isTravelling = MutableLiveData<Boolean>(false)
    val isTravelling: LiveData<Boolean> = _isTravelling

    private val _timeRunInSeconds = MutableLiveData<Long>()
    val timeRunInSeconds: LiveData<Long> = _timeRunInSeconds

    val pathPoints = repository.pathPoints

    val distanceFromStartKm = repository.distanceFromStartKm

    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    fun initService() {
        val serviceIntent = Intent(context, ForegroundOnlyLocationService::class.java)
        context.bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener (this)
    }

    fun finishService() {
        if (foregroundOnlyLocationServiceBound) {
            context.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            _isTracking.postValue(isForegroundEnabled)
        }
    }

    private val isForegroundEnabled
        get() = sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

    val initLocation: Task<Location> = repository.lastLocation

    fun startTracking() {
        Log.d("OvO", "ViewModel::startTracking")
        foregroundOnlyLocationService?.subscribeToLocationUpdates(repository)
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
        repository.startTrackLocation()
    }

    fun endTrip() {
        Log.d("OvO", "ViewModel::endTrip")
        repository.endTrackLocation()
        _isTravelling.value = false
    }

    fun endTracking() {
        foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
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

    val liveCurrentTrip: LiveData<Trip> get() = repository.gpsTracker.liveCurrentTrip

    fun flush() {
        repository.gpsTracker.flush()
    }
    fun saveTrip() {
        repository.gpsTracker.saveTrip()
    }
}