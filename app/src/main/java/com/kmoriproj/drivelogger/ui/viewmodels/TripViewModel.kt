package com.kmoriproj.drivelogger.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmoriproj.drivelogger.common.SortType
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    val tripRepository: TripRepository
) : ViewModel() {
    private val tripsSortedByDate = tripRepository.getAllTripsByDate()
    private val tripsSortedByStartTime = tripRepository.getAllTripsByStartTime()
    private val tripsSortedByFarest = tripRepository.getAllTripsByFarest()
    private val tripsSortedByNearest = tripRepository.getAllTripsByNearest()

    val trips = MediatorLiveData<List<Trip>>()

    var sortType = SortType.DATE

    init {
        trips.addSource(tripsSortedByDate) { result ->
            Timber.d("TRIPS SORTED BY Date")
            if (sortType == SortType.DATE) {
                result?.let { trips.value = it }
            }
        }
        trips.addSource(tripsSortedByStartTime) { result ->
            Timber.d("TRIPS SORTED BY StartTime")
            if (sortType == SortType.STARTTIME) {
                result?.let { trips.value = it }
            }
        }
        trips.addSource(tripsSortedByFarest) { result ->
            Timber.d("TRIPS SORTED BY Farest")
            if (sortType == SortType.FAREST) {
                result?.let { trips.value = it }
            }
        }
        trips.addSource(tripsSortedByNearest) { result ->
            Timber.d("TRIPS SORTED BY Nearest")
            if (sortType == SortType.NEAREST) {
                result?.let { trips.value = it }
            }
        }
    }
    fun sortRuns(sortType: SortType) = when(sortType) {
        SortType.DATE -> tripsSortedByDate.value?.let { trips.value = it }
        SortType.STARTTIME -> tripsSortedByStartTime.value?.let { trips.value = it }
        SortType.FAREST -> tripsSortedByFarest.value?.let { trips.value = it }
        SortType.NEAREST -> tripsSortedByNearest.value?.let { trips.value = it }
    }.also {
        this.sortType = sortType
    }

    fun insertTrip(trip: Trip) = viewModelScope.launch {
        tripRepository.insertTrip(trip)
    }

    fun deleteTrip(trip: Trip) = viewModelScope.launch {
        tripRepository.deleteTrip(trip)
    }
}