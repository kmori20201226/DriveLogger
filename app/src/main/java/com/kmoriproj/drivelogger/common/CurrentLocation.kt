package com.kmoriproj.drivelogger.common

import com.google.android.gms.maps.model.LatLng

data class CurrentLocation (
    val latlng: LatLng,
    val travelDistance: Float,
    val travelTime: Long
    ){
}