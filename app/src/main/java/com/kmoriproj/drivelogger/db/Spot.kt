package com.kmoriproj.drivelogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "spot")
data class Spot(
    val tripId: Long = 0,
    val spotName: String? = null,
    val stayTime: Long = 0,
    val point: LatLng = LatLng(0.0, 0.0),
    val radius: Float = 0.0f,
    val entered: Long = 0,
    val leaved: Long = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}