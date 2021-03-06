package com.kmoriproj.drivelogger.ui.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.MarkerOptions
import com.kmoriproj.drivelogger.common.LocationSnapshot
import com.kmoriproj.drivelogger.common.RichPoint
import com.kmoriproj.drivelogger.common.distanceTo
import com.kmoriproj.drivelogger.db.*
import com.kmoriproj.drivelogger.repositories.SpotRepository
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    val trajectoryRepository: TrajectoryRepository,
    val tripRepository: TripRepository,
    val spotRepository: SpotRepository
) : ViewModel() {

    private val _points = MutableLiveData<List<RichPoint>>()
    val points: LiveData<List<RichPoint>> = _points
    private val _currentTrip = MutableLiveData<Trip>()
    val currentTrip: LiveData<Trip> = _currentTrip
    private val _spots = MutableLiveData<List<Spot>>()
    val spots: LiveData<List<Spot>> = _spots

    private fun trajPointList2LocationSnapshotList(
        trpRec: Trip,
        t: TrajPointList
    ): List<LocationSnapshot> {
        return mutableListOf<LocationSnapshot>().also {
            var lastPt: TrajPoint? = null
            var travelDistance: Float = 0.0f
            var travelTime: Long = 0
            t.trajPoint.forEach { pt ->
                val distanceInMeter = if (lastPt == null) {
                    0.0f
                } else {
                    pt.point.distanceTo(lastPt!!.point)
                }
                val timeSpan = if (lastPt == null) 0 else pt.time - lastPt!!.time
                lastPt = pt
                val curloc = LocationSnapshot(
                    time = pt.time,
                    speed = pt.speed ?: 0.0f,
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

    private fun locationSnapshotList2RichPointList(locSnapshots: List<LocationSnapshot>): List<RichPoint> {
        return mutableListOf<RichPoint>().also {
            for (ls in locSnapshots) {
                it.add(RichPoint.makeFrom(ls, it))
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
                        val t2 = trajPointList2LocationSnapshotList(tripRec, t1)
                        val t3 = locationSnapshotList2RichPointList(t2)
                        _points.postValue(t3)
                    }
                }
            }
        }
        spotRepository.getSpotsByTripId(tripId).observe(owner) {
                spotList ->
                _spots.postValue(spotList)
        }
    }
}