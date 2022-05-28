package com.kmoriproj.drivelogger.db

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

// c.f. https://medium.com/@nemanja.stamenovic/how-to-use-room-with-nested-json-81dec1df1908

data class TrajPoint(
    var time: Long = 0,
    var accuracy: Float? = null,
    var altitude: Double? = null,
    var bearing: Float? = null,
    var bearingAccuracyDegrees: Float? = null,
    var point: LatLng = LatLng(0.0, 0.0),
    var speed: Float? = null,
    var speedAccuracyMetersPerSecond: Float? = null,
    var verticalAccuracyMeters: Float? = null
) {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun fromLocation(loc: Location): TrajPoint {
            return TrajPoint(
                time = loc.time,
                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                bearing = if (loc.hasBearing()) loc.bearing else null,
                bearingAccuracyDegrees = if (loc.hasBearingAccuracy()) loc.bearingAccuracyDegrees else null,
                point = LatLng(loc.latitude, loc.longitude),
                speed = if (loc.hasSpeed()) loc.speed else null,
                speedAccuracyMetersPerSecond = if (loc.hasSpeedAccuracy()) loc.speedAccuracyMetersPerSecond else null,
                verticalAccuracyMeters = if (loc.hasVerticalAccuracy()) loc.verticalAccuracyMeters else null
            )
        }
    }
}

data class TrajPointList constructor(
    val trajPoint: List<TrajPoint>
) {
    private fun getBoundingBox(ptList: List<TrajPoint>): BoundingBox {
        var min_x = Double.MAX_VALUE
        var max_x = Double.MIN_VALUE
        var min_y = Double.MAX_VALUE
        var max_y = Double.MIN_VALUE
        for (p in ptList) {
            min_x = min_x.coerceAtLeast(p.point.longitude)
            max_x = max_x.coerceAtMost(p.point.longitude)
            min_y = min_y.coerceAtLeast(p.point.latitude)
            max_y = max_y.coerceAtMost(p.point.latitude)
        }
        return BoundingBox(LatLng(min_y, min_x), LatLng(max_y, max_x))
    }
    val startTime: Long get() = trajPoint.first().time
    val endTime: Long get() = trajPoint.last().time
    val boundingBox: BoundingBox get() = getBoundingBox(trajPoint)
}

@Entity(tableName = "trajectories")
data class Trajectory(
    var tripId: Long? = null,
    var startTime: Long = 0,
    var endTime: Long = 0,
    var bbox: BoundingBox = BoundingBox(LatLng(0.0, 0.0), LatLng(0.0, 0.0)),
    @Embedded
    var trajPoints: TrajPointList,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
    init {
        startTime = trajPoints.startTime
        endTime = trajPoints.endTime
        bbox = trajPoints.boundingBox
    }
    val size: Int get() = trajPoints.trajPoint.size
}

