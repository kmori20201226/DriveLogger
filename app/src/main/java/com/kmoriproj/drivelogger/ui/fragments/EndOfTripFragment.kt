package com.kmoriproj.drivelogger.ui.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.kmoriproj.drivelogger.EndOfTripViewModel
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.databinding.DrivingFragmentBinding
import com.kmoriproj.drivelogger.databinding.FragmentEndOfTripBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EndOfTripFragment : Fragment(R.layout.fragment_end_of_trip) {

    companion object {
        fun newInstance() = EndOfTripFragment()
    }

    private lateinit var binding: FragmentEndOfTripBinding
    private val viewModel: EndOfTripViewModel by viewModels()

    val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEndOfTripBinding.bind(view)
        binding.btnSave.setOnClickListener {
            viewModel.liveCurrentTrip.value?.caption = binding.txComments.text?.toString()!!
            viewModel.finishTrip()
            activity?.onBackPressed()
        }
        fun formatTime(t:Long): String {
            val s = Date(t)
            return timestampFormat.format(s)
        }
        viewModel.liveCurrentTrip.observe(viewLifecycleOwner) {
            with( viewModel.liveCurrentTrip.value ) {
                binding.tvStartTime.text = formatTime(it.startTime)
                binding.tvEndTime.text = formatTime(it.endTime)
                binding.tvDistanceFromStart.text = "%.1fkm".format(it.distanceFromStart / 1000.0)
                binding.tvNumPoints.text = "%d".format(it.numDataPoints)
            }
        }
    }
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_end_of_trip, container, false)
//    }
//
//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        // viewModel = ViewModelProvider(this).get(EndOfTripViewModel::class.java)
//        // TODO: Use the ViewModel
//    }

}