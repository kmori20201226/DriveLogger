package com.kmoriproj.drivelogger.ui

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kmoriproj.drivelogger.common.CurrentLocation
import com.kmoriproj.drivelogger.common.RichPoint
import com.kmoriproj.drivelogger.db.TrajPoint
import com.kmoriproj.drivelogger.db.TrajPointList
import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    val trajectoryRepository: TrajectoryRepository,
    val tripRepository: TripRepository
) : ViewModel() {

    private val _points = MutableLiveData<List<RichPoint>>()
    val points: LiveData<List<RichPoint>> = _points
    private val _currentTrip = MutableLiveData<Trip>()
    val currentTrip: LiveData<Trip> = _currentTrip

    private fun trajPointList2CurrentLocationList(
        trpRec: Trip,
        t: TrajPointList
    ): List<CurrentLocation> {
        return mutableListOf<CurrentLocation>().also {
            var lastPt: TrajPoint? = null
            var travelDistance: Float = 0.0f
            var travelTime: Long = 0
            t.trajPoint.forEach { pt ->
                val distanceInMeter = if (lastPt == null) {
                    0.0f
                } else {
                    val d = FloatArray(1)
                    Location.distanceBetween(
                        pt.point.latitude, pt.point.longitude,
                        lastPt!!.point.latitude, lastPt!!.point.longitude,
                        d
                    )
                    d[0]
                }
                val timeSpan = if (lastPt == null) 0 else pt.time - lastPt!!.time
                lastPt = pt
                val curloc = CurrentLocation(
                    time = pt.time,
                    latlng = pt.point,
                    distanceInMeter = distanceInMeter,
                    timeSpan = timeSpan,
                    travelDistance = travelDistance,
                    travelTime = travelTime,
                )
                travelDistance += distanceInMeter
                travelTime += travelTime
                it.add(curloc)
            }
        }
    }

    private fun currentLocationList2RichPointList(v: List<CurrentLocation>): List<RichPoint> {
        return mutableListOf<RichPoint>().also {
            for (curloc in v) {
                it.add(RichPoint.makeFrom(curloc, it))
            }
        }
    }

    fun trajectoryToTrajPointList(v: List<Trajectory>): List<TrajPointList> {
        return v.map { it.trajPoints }
    }

    fun getTrip(tripId: Long, owner: LifecycleOwner) {
        tripRepository.getTrip(tripId).observe(owner) {
            tripRec ->
            _currentTrip.postValue(tripRec)
            viewModelScope.launch {
                val trajFlow = trajectoryRepository.getTrajectoriesOfTrip(tripId)
                trajFlow.collect {
                    val trajPointList = trajectoryToTrajPointList(it)
                    if (trajPointList.size > 0) {
                        val t1 = trajPointList.reduce { acc, v -> TrajPointList(acc, v) }
                        val t2 = trajPointList2CurrentLocationList(tripRec, t1)
                        val t3 = currentLocationList2RichPointList(t2)
                        _points.postValue(t3)
                    }
                }
            }
        }
    }
}