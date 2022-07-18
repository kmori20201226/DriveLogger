package com.kmoriproj.drivelogger.common

import com.google.android.gms.maps.model.LatLng

data class LocationSnapshot (
    val time: Long,
    val speed: Float,
    val latlng: LatLng,
    val travelDistance: Float,
    val travelTime: Long,
    val distanceInMeter: Float,
    val timeSpan: Long
    ){
}