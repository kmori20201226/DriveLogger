package com.kmoriproj.drivelogger.common

import android.location.Location
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MovementTracker {
    private var writer: CsvFileWriter? = null;
    var lastPos: LatLng? = null
    val locationList = mutableListOf<Location>()
    var distanceFromStart = 0.0
    private fun openDumpfile() {
        var dir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS).absolutePath + "/DriveLogger")
        dir.mkdirs()
        val now = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val file = File(dir, "location-dump-%s.csv".format(now))
        @OptIn(KotlinCsvExperimental::class)
        writer = csvWriter().openAndGetRawWriter(file)
        writer?.writeRow(header())
    }
    private fun header(): List<String> {
        return listOf(
            "time",
            "hasAccuracy",
            "accuracy",
            "hasAltitude",
            "altitude",
            "hasBearing",
            "bearing",
            "hasBearingAccuracy",
            "bearingAccuracyDegrees",
            "hasElapsedRealtimeUncertaintyNanos",
            "elapsedRealtimeUncertaintyNanos",
            "elapsedRealtimeNanos",
            "elapsedRealtimeUncertaintyNanos",
            "latitude",
            "longitude",
            "provider",
            "hasSpeed",
            "speed",
            "hasSpeedAccuracy",
            "speedAccuracyMetersPerSecond",
            "hasVerticalAccuracy",
            "verticalAccuracyMeters",
        )
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun addLocation(loc: Location) : Boolean {
        val pos = LatLng(loc.latitude, loc.longitude)
        val row = listOf(
            loc.time,
            loc.hasAccuracy(),
            loc.accuracy,
            loc.hasAltitude(),
            loc.altitude,
            loc.hasBearing(),
            loc.bearing,
            loc.hasBearingAccuracy(),
            loc.bearingAccuracyDegrees,
            loc.hasElapsedRealtimeUncertaintyNanos(),
            loc.elapsedRealtimeUncertaintyNanos,
            loc.elapsedRealtimeNanos,
            loc.elapsedRealtimeUncertaintyNanos,
            loc.latitude,
            loc.longitude,
            loc.provider,
            loc.hasSpeed(),
            loc.speed,
            loc.hasSpeedAccuracy(),
            loc.speedAccuracyMetersPerSecond,
            loc.hasVerticalAccuracy(),
            loc.verticalAccuracyMeters,
        )
        if (writer == null) {
            openDumpfile()
        }
        writer?.writeRow(row)
        if (lastPos == null) {
            lastPos = pos
            locationList.add(loc)
            return true
        } else {
            val result = FloatArray(1)
            Location.distanceBetween(
                pos.latitude, pos.longitude,
                lastPos!!.latitude, lastPos!!.longitude,
                result)
            val distanceInMeter = result[0]
            if (distanceInMeter >= 2.0) {
                lastPos = pos
                locationList.add(loc)
                distanceFromStart += distanceInMeter
                return true
            }
        }
        return false
    }
    fun close() {
        writer?.close()
        writer = null
    }
    protected fun finalize() {
        close()
    }
}