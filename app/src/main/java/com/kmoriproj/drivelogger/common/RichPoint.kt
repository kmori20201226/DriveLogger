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
        fun makeFrom(ls: LocationSnapshot, prevPoints: Polyline) : RichPoint {
            val speedInKmPerH = if (ls.timeSpan > 0)
                (ls.distanceInMeter / 1000.0) / (ls.timeSpan / 3600000.0)
                    else 0
            var t1= ls.time
            for (p in prevPoints.reversed()) {
                val d = FloatArray(1)
                Location.distanceBetween(
                    ls.latlng.latitude, ls.latlng.longitude,
                    p.latlng.latitude, p.latlng.longitude,
                    d)
                if (d[0] > 100.0) {
                    break
                }
                t1 = p.time
            }
            return RichPoint(
                ls.time,
                latlng = ls.latlng,
                speed = speedInKmPerH.toFloat(),
                staleMinutes = ls.time - t1
            )
        }
    }
}