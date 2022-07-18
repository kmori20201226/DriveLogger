package com.kmoriproj.drivelogger.common

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun LatLng.distanceTo(v: LatLng): Float {
    val d = FloatArray(1)
    Location.distanceBetween(
        this.latitude, this.longitude,
        v.latitude, v.longitude,
        d)
    return d[0]
}