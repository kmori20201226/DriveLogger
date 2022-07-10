package com.kmoriproj.drivelogger.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.DateTimeString
import com.kmoriproj.drivelogger.databinding.FragmentEndOfTripBinding
import com.kmoriproj.drivelogger.ui.viewmodels.DrivingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EndOfTripFragment : Fragment(R.layout.fragment_end_of_trip) {

    companion object {
        fun newInstance() = EndOfTripFragment()
    }

    private lateinit var binding: FragmentEndOfTripBinding

    private val viewModel: DrivingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEndOfTripBinding.bind(view)
        binding.btnSave.setOnClickListener {
            viewModel.endTrip()
            viewModel.saveTrip()
            viewModel.liveCurrentTrip.value?.caption = binding.txComments.text?.toString()!!
            findNavController().navigate(R.id.action_endOfTripFragment_to_tripsFragment)
        }
        viewModel.flush()
        viewModel.liveCurrentTrip.observe(viewLifecycleOwner) {
            with( viewModel.liveCurrentTrip.value ) {
                binding.tvStartTime.text = DateTimeString.formatDateTime(it.startTime)
                binding.tvEndTime.text = DateTimeString.formatDateTime(it.endTime)
                binding.tvDistanceFromStart.text = "%.1fkm".format(it.distanceFromStart / 1000.0)
                binding.tvNumPoints.text = "%d".format(it.numDataPoints)
            }
        }
    }
}

