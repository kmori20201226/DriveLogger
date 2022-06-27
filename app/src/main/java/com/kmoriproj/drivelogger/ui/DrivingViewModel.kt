package com.kmoriproj.drivelogger.ui

import android.content.*
import android.location.Location
import android.os.IBinder
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.LocationRepository
import com.kmoriproj.drivelogger.services.ForegroundOnlyLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DrivingViewModel @Inject constructor(
        val context: Context,
        private val repository: LocationRepository) : ViewModel() {

    val timeRunInSeconds = repository.timeRunInSeconds

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

    fun initService() {
        val serviceIntent = Intent(context, ForegroundOnlyLocationService::class.java)
        context.bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        repository.initialize()
    }

    fun finishService() {
        if (foregroundOnlyLocationServiceBound) {
            context.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        repository.finalize()
    }

    val isTravelling = repository.isTravelling
    val isTracking = repository.isTracking
    fun getLastLocation(): Task<Location> = repository.getLastLocation()

    fun startTracking() {
        foregroundOnlyLocationService?.subscribeToLocationUpdates(repository)
        repository.startTracking()
    }

    fun endTrip() = repository.endTrip()

    fun endTracking() {
        foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
        repository.endTracking()
    }

    fun pauseTracking() = repository.pauseTracking()

    fun resumeTracking()  = repository.resumeTracking()

    val liveCurrentTrip: LiveData<Trip> get() = repository.gpsTracker.liveCurrentTrip

    fun flush() = repository.flush()

    fun saveTrip() = repository.saveTrip()
}