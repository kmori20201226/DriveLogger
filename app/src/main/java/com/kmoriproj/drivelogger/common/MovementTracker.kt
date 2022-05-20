package com.kmoriproj.drivelogger.common

import android.location.Location
import com.google.android.gms.maps.model.LatLng

class MovementTracker {
    var lastPos: LatLng? = null
    val locationList = mutableListOf<Location>()
    var distanceFromStart = 0.0
    fun addLocation(loc: Location) : Boolean {
        val pos = LatLng(loc.latitude, loc.longitude)
        if (lastPos == null) {
            lastPos = pos
            locationList.add(loc)
            return true
        } else {
            val result = FloatArray(1)
            Location.distanceBetween(
                pos.latitude, pos.longitude,
                lastPos!!.latitude, lastPos!!.longitude,
                result)
            val distanceInMeter = result[0]
            if (distanceInMeter >= 2.0) {
                lastPos = pos
                locationList.add(loc)
                distanceFromStart += distanceInMeter
                return true
            }
        }
        return false
    }
}