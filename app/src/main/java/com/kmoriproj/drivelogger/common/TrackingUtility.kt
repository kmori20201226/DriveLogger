package com.kmoriproj.drivelogger.common

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.kmoriproj.drivelogger.common.Polyline
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

class TrackingUtility {

    companion object {
        /**
         * Checks if the user accepted the necessary location permissions or not
         */
        fun hasLocationPermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }
}