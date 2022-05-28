package com.kmoriproj.drivelogger.db

import com.google.android.gms.maps.model.LatLng

data class BoundingBox(
    val ll: LatLng,
    val ur: LatLng
) {
    fun extend(bb: BoundingBox): BoundingBox {
        val arr = arrayOf(ll, ur, bb.ll, bb.ur)
        return BoundingBox(
            LatLng(arr.minOf { it.latitude }, arr.minOf { it.longitude } ),
            LatLng(arr.maxOf { it.latitude }, arr.maxOf { it.longitude})
        )
    }
}