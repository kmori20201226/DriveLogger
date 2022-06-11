package com.kmoriproj.drivelogger.common

import android.graphics.Color
import android.location.LocationManager

class Constants {

    companion object {
        const val BUNDLE_KEY_MAPVIEW = "MapViewBundleKey"
        const val BUNDLE_KEY_POINT_IX = "PointIx1BundleKey"
        const val BUNDLE_KEY_POINT_IX2 = "PointIx2BundleKey"

        const val REQUEST_CODE_LOCATION_PERMISSION = 0

        const val ARGKEY_CURRENT_TRIPID = "CurrentTripId"

        // Database
        const val DATABASE_NAME = "running_db"

        // Tracking Options
        const val LOCATION_UPDATE_INTERVAL = 5000L
        const val FASTEST_LOCATION_UPDATE_INTERVAL = 2000L

        // Map Options
        const val POLYLINE_COLOR = Color.RED
        const val POLYLINE_WIDTH = 8f
        const val MAP_ZOOM = 15f

        // Timer
        const val TIMER_UPDATE_INTERVAL = 50L

        // MapView
        const val MAP_VIEW_HEIGHT_IN_DP = 200f

        // Notifications
        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Tracking"
        const val NOTIFICATION_ID = 1

        // Shared Preferences
        const val SHARED_PREFERENCES_NAME = "sharedPref"
        const val KEY_NAME = "KEY_NAME"
        const val KEY_WEIGHT = "KEY_WEIGHT"
        const val KEY_FIRST_TIME_TOGGLE = "KEY_FIRST_TIME_TOGGLE"

        // Service Intent Actions
        const val ACTION_INIT_LOCATION = "ACTION_INIT_LOCATION"
        const val ACTION_SHOW_TRACKING_FRAGMENT = "ACTION_SHOW_TRACKING_FRAGMENT"
        const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }
}