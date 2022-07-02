package com.kmoriproj.drivelogger.common

import com.google.android.gms.maps.model.LatLng

data class CurrentLocation (
    val time: Long,
    val latlng: LatLng,
    val travelDistance: Float,
    val travelTime: Long,
    val distanceInMeter: Float,
    val timeSpan: Long
    ){
}