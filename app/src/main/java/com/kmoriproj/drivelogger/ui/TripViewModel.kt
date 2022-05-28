package com.kmoriproj.drivelogger.ui

import androidx.lifecycle.ViewModel
import com.kmoriproj.drivelogger.repositories.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    val tripRepository: TripRepository
) : ViewModel() {
}