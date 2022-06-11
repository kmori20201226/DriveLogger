package com.kmoriproj.drivelogger.ui

import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.kmoriproj.drivelogger.db.TrajPoint
import com.kmoriproj.drivelogger.db.TrajPointList
import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrajectoryViewModel @Inject constructor(
    val trajectoryRepository: TrajectoryRepository
) : ViewModel() {

    private val _points = MutableLiveData<List<LatLng>>()
    val points : LiveData<List<LatLng>> = _points

    private fun trajPointList2LatLng(t: TrajPointList): List<LatLng> {
        return mutableListOf<LatLng>().also {
            t.trajPoint.forEach { pt -> it.add(pt.point) }
        }
    }
    fun trajectoryToTrajPointList(v: List<Trajectory>) : List<TrajPointList> {
        return v.map { it.trajPoints }
    }
    fun trajectoryOf(tripId: Long) {
        viewModelScope.launch {
            val x = trajectoryRepository.getTrajectoriesOfTrip(tripId)
            x.collect {
                val xx: List<Trajectory> = it
                val trajPointList = trajectoryToTrajPointList(xx)
                if (trajPointList.size > 0) {
                    val t1 = trajPointList.reduce { acc, v -> TrajPointList(acc, v) }
                    val t2 = trajPointList2LatLng(t1)
                    _points.postValue(t2)
                }
            }
        }
    }

}