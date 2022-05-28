package com.kmoriproj.drivelogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip")
data class Trip(
    var startTime: Long = 0,
    var endTime: Long = 0,
    var caption: String = "",
    var bbox: BoundingBox? = null,
    var distanceFromStart: Float = 0.0f,
    var numDataPoints: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}