package com.kmoriproj.drivelogger.db

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    public fun fromBoundingBox(bb: BoundingBox?): String? {
        if (bb == null) {
            return null
        }
        return "%f,%f,%f,%f".format(bb.ll.longitude, bb.ll.latitude, bb.ur.longitude, bb.ur.latitude)
    }
    @TypeConverter
    public fun toBoundingBox(s: String?): BoundingBox? {
        if (s == null) {
            return null
        }
        val v = s.split(",").map{
            it.toDouble()
        }
        return BoundingBox(LatLng(v[0], v[1]), LatLng(v[2], v[3]))
    }
    @TypeConverter
    public fun fromLatLng(v: LatLng): String {
        return "%f,%f".format(v.longitude, v.latitude)
    }
    @TypeConverter
    public fun toLatLng(s: String): LatLng {
        val v = s.split(",").map{
            it.toDouble()
        }
        return LatLng(v[0], v[1])
    }
    @TypeConverter
    fun fromTrajPointList(trajPoints: List<TrajPoint>): String {
        val type = object : TypeToken<List<TrajPoint>>() {}.type
        return Gson().toJson(trajPoints, type)
    }
    @TypeConverter
    fun toTrajPointList(trajPointsString: String): List<TrajPoint> {
        val type = object : TypeToken<List<TrajPoint>>() {}.type
        return Gson().fromJson<List<TrajPoint>>(trajPointsString, type)
    }
}