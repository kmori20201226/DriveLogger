package com.kmoriproj.drivelogger.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.common.Constants.Companion.FASTEST_LOCATION_UPDATE_INTERVAL
import com.kmoriproj.drivelogger.common.Constants.Companion.LOCATION_UPDATE_INTERVAL
import com.kmoriproj.drivelogger.common.LocationSnapshot
import com.kmoriproj.drivelogger.common.GPSTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

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

        priority = Priority.PRIORITY_HIGH_ACCURACY
    }
    init {
        Log.d("OvO", "SharedLocationManager created!!!")
    }

    private val job = Job()
    private val coroutinesScope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private var _locationFlow = callbackFlow<LocationSnapshot> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                coroutinesScope.launch {
                    gps.addLocation(result.lastLocation!!)?.also {
                        trySend(it)
                    }
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            Log.e("OvO", e.toString())
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d("OvO", "closeTrip!!!")
            fusedLocationProviderClient.removeLocationUpdates(callback) // clean up when Flow collection ends
            coroutinesScope.launch {
                gps.flush()
            }
        }
    }.shareIn(
        (context.applicationContext as BaseApplication).applicationScope,
        replay = 0,
        started = SharingStarted.Lazily
    )

    val tripId: Long?
        get() = gps.currentTrip?.id

    @ExperimentalCoroutinesApi
    val locationFlow: Flow<LocationSnapshot>
        get() = _locationFlow

    fun getLastLocation(): Task<Location>
         = fusedLocationProviderClient.lastLocation
}