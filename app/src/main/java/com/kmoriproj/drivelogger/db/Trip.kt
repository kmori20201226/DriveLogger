package com.kmoriproj.drivelogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip")
data class Trip constructor (
    var startTime: Long,
    var endTime: Long,
    var caption: String,
    var bbox: BoundingBox?,
    var distanceFromStart: Float,
    var numDataPoints: Int
) {
    constructor() : this(0, 0, "", null, 0.0f, 0)
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}