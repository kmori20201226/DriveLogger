package com.kmoriproj.drivelogger.common

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR1
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR2
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR3

class RichPoint(
    val time: Long,
    val latlng: LatLng,
    val speed: Float,
    val staleMinutes: Long
) {
    val color : Int
        get() =
            if (speed <= 10.0)
                POLYLINE_COLOR1
            else if (speed <= 50.0)
                POLYLINE_COLOR2
            else
                POLYLINE_COLOR3

    companion object {
        fun makeFrom(curloc: CurrentLocation, prevPoints: Polyline) : RichPoint {
            val speedInKmParH = if (curloc.timeSpan > 0)
                (curloc.distanceInMeter / 1000.0) / (curloc.timeSpan / 3600000.0)
                    else 0
            var t1= curloc.time
            for (p in prevPoints.reversed()) {
                val d = FloatArray(1)
                Location.distanceBetween(
                    curloc.latlng.latitude, curloc.latlng.longitude,
                    p.latlng.latitude, p.latlng.longitude,
                    d)
                if (d[0] > 100.0) {
                    break
                }
                t1 = p.time
            }
            return RichPoint(
                curloc.time,
                latlng = curloc.latlng,
                speed = speedInKmParH.toFloat(),
                staleMinutes = curloc.time - t1
            )
        }
    }
}