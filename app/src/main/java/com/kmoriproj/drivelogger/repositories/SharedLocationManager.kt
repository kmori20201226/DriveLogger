package com.kmoriproj.drivelogger.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.common.Constants.Companion.FASTEST_LOCATION_UPDATE_INTERVAL
import com.kmoriproj.drivelogger.common.Constants.Companion.LOCATION_UPDATE_INTERVAL
import com.kmoriproj.drivelogger.common.CurrentLocation
import com.kmoriproj.drivelogger.common.GPSTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SharedLocationManager(
    private val context: Context,
    val gps: GPSTracker
) {
    // TODO: Step 1.2, Review the FusedLocationProviderClient.
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // TODO: Step 1.3, Create a LocationRequest.
    private val locationRequest = LocationRequest.create().apply {
        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them less frequently than requested. You may also receive updates more
        // frequently than requested if other applications are requesting location at a more
        // frequent interval.
        //
        // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
        // targetSdkVersion) may receive updates less frequently than this interval when the app
        // is no longer in the foreground.
        interval = LOCATION_UPDATE_INTERVAL

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates more frequently than this value.
        fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        // maxWaitTime = 60000

        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    init {
        Log.d("OvO", "SharedLocationManager created!!!")
    }
    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private var _locationFlow = callbackFlow<CurrentLocation> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Timber.d("Location change received")
                gps.addLocation(result.lastLocation)?.also {
                    trySend(it)
                }
            }
        }
        Log.d("OvO",  "startTrip!!!")
        gps.startTrip()
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            Timber.e(e)
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d("OvO", "closeTrip!!!")
            fusedLocationProviderClient.removeLocationUpdates(callback) // clean up when Flow collection ends
            gps.flush()
        }
    }.shareIn(
        (context.applicationContext as BaseApplication).applicationScope,
        replay = 0,
        started = SharingStarted.Lazily
    )

    @ExperimentalCoroutinesApi
    val locationFlow: Flow<CurrentLocation>
        get() = _locationFlow

    fun getLastLocation(): Task<Location>
         = fusedLocationProviderClient.lastLocation
}