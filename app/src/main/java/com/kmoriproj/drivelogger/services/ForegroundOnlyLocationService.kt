/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kmoriproj.drivelogger.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.DateTimeString
import com.kmoriproj.drivelogger.common.LocationSnapshot
import com.kmoriproj.drivelogger.repositories.LocationRepository
import com.kmoriproj.drivelogger.ui.MapsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 *
 * Code sampled from
 * https://barbeau.medium.com/kotlin-callbackflow-a-lightweight-architecture-for-location-aware-android-apps-f97b5e66aaa2
 * but heavily modified.
 */
@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {

    private var locationFlowJob: Job? = null
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    // TODO: Step 1.1, Review variables (no changes).
    // FusedLocationProviderClient - Main class for receiving location updates.
    //private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    //private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    //private lateinit var locationCallback: LocationCallback

    //val locationFlow : Flow<Location> = flowOf<Location>()
    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    override fun onCreate() {
        Timber.d("ForegroundOnlyLocationService onCreate()")
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("ForegroundOnlyLocationService onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)
                ?: false

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
        return super.onStartCommand(intent, flags, START_NOT_STICKY)
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("ForegroundOnlyLocationService onBind()")

        if (false) {
            // MainActivity (client) comes into foreground and binds to service, so the service can
            // become a background services.
            stopForeground(true)
            serviceRunningInForeground = false
            configurationChange = false
        }
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Timber.d("ForegroundOnlyLocationService onRebind()")
        if (false) {
            // MainActivity (client) returns to the foreground and rebinds to service, so the service
            // can become a background services.
            stopForeground(true)
            serviceRunningInForeground = false
            configurationChange = false
        }
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("ForegroundOnlyLocationService onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Timber.d("Start foreground service")
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Timber.d("ForegroundOnlyLocationService onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates(repository: LocationRepository) {
        Timber.d("ForegroundOnlyLocationService subscribeToLocationUpdates()")

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        // Observe locations via Flow as they are generated by the repository
        locationFlowJob = repository.locationFlow
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    generateNotification(it))
            }
            .launchIn(lifecycleScope)
        val notification = generateNotification(null)
        startForeground(NOTIFICATION_ID, notification)
        serviceRunningInForeground = true
    }

    fun unsubscribeToLocationUpdates() {
        Timber.d("ForegroundOnlyLocationService unsubscribeToLocationUpdates()")
        stopForeground(0)
        SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun generateNotification(location: LocationSnapshot?): Notification {
        Timber.d("generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = if (location == null) {
            getString(R.string.no_location_text)
        } else {
            val distance = location.travelDistance / 1000
            val duration = DateTimeString.elapsed(location.travelTime)
            "%.1fkm %s".format(distance, duration)
        }
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_LOW)

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MapsActivity::class.java)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.ic_directions_car_48px)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val PACKAGE_NAME = "com.kmoriproj.drivelogger.services"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
    }
}
