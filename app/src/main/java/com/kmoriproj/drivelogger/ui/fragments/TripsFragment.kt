package com.kmoriproj.drivelogger.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.adapters.TripsAdapter
import com.kmoriproj.drivelogger.common.Constants.Companion.ARGKEY_CURRENT_TRIPID
import com.kmoriproj.drivelogger.common.Constants.Companion.KEY_DR_SERVER_URL
import com.kmoriproj.drivelogger.common.SortType
import com.kmoriproj.drivelogger.databinding.FragmentTripsBinding
import com.kmoriproj.drivelogger.db.Trip
import com.kmoriproj.drivelogger.repositories.TrajectoryRepository
import com.kmoriproj.drivelogger.server_interaction.DriveLogUploadService
import com.kmoriproj.drivelogger.server_interaction.UploadBody
import com.kmoriproj.drivelogger.ui.TrajectoryViewModel
import com.kmoriproj.drivelogger.ui.TripViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class TripsFragment : Fragment(R.layout.fragment_trips) {
    lateinit var tripsAdapter: TripsAdapter

    private val tripViewModel: TripViewModel by viewModels()
    private val trajectoryViewModel: TrajectoryViewModel by viewModels()

    private val uploadSuccessMessage = MutableLiveData("")
    private val uploadErrorMessage = MutableLiveData("")

    private lateinit var binding: FragmentTripsBinding

    private lateinit var server_url: String

    private fun upload(trip: Trip, trajectoryRepository: TrajectoryRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            val retrofit = Retrofit.Builder()
                .baseUrl(server_url)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            val uploadService = retrofit.create(DriveLogUploadService::class.java)
            withContext(Dispatchers.IO) {
                val listTrajectory =
                    trajectoryRepository.getBlockedTrajectoriesOfTrip(
                        trip.id!!
                    )
                val body = UploadBody(
                    trip,
                    listTrajectory
                )
                try {
                    val reply = uploadService.upload(body).execute()
                    if (reply.isSuccessful) {
                        uploadSuccessMessage.postValue(getString(R.string.upload_succeeded))
                    } else {
                        uploadErrorMessage.postValue(getString(R.string.upload_server_error))
                    }
                } catch (th: Throwable) {
                    uploadErrorMessage.postValue(getString(R.string.network_error) + " " + th.toString())
                    // java.net.UnknownServiceException: CLEARTEXT communication to 192.168.10.115 not permitted by network security policy
                    // java.net.SocketTimeoutException: failed to connect to /192.168.10.115 (port 8880) from /10.0.2.16 (port 48846) after 10000ms
                    Log.e("OvO", "UploadFailed", th)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel = (activity as MainActivity).mainViewModel
        tripsAdapter = TripsAdapter {
            subAction, trip ->
            when(subAction) {
                TripsAdapter.subActionDefault -> {
                    val bundle = Bundle()
                    bundle.putLong(ARGKEY_CURRENT_TRIPID, trip.id!!)
                    findNavController().navigate(
                        R.id.action_tripsFragment_to_reviewFragment,
                        bundle
                    )
                }
                TripsAdapter.subActionUpload -> {
                    if (server_url == "") {
                        Snackbar
                            .make(binding.root, getString(R.string.server_url_unknown), LENGTH_LONG)
                            .show()
                    } else {
                        upload(trip, trajectoryViewModel.trajectoryRepository)
                    }
                }
            }
        }

        val default_url = getString(R.string.default_dr_server_url)
        server_url = context
            ?.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            ?.getString(KEY_DR_SERVER_URL, default_url) ?: default_url

        binding = FragmentTripsBinding.bind(view)

        setupRecyclerView()

        when (tripViewModel.sortType) {
            SortType.DATE -> binding.spFilter.setSelection(0)
            SortType.STARTTIME -> binding.spFilter.setSelection(1)
            SortType.FAREST -> binding.spFilter.setSelection(2)
            SortType.NEAREST -> binding.spFilter.setSelection(3)
        }
        tripViewModel.trips.observe(viewLifecycleOwner) { trips ->
            tripsAdapter.submitList(trips)
        }

        binding.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}

            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> tripViewModel.sortRuns(SortType.DATE)
                    1 -> tripViewModel.sortRuns(SortType.STARTTIME)
                    2 -> tripViewModel.sortRuns(SortType.FAREST)
                    3 -> tripViewModel.sortRuns(SortType.NEAREST)
                }
            }
        }
        uploadSuccessMessage.observe(viewLifecycleOwner) {
            if (it != "") {
                Snackbar.make(view, it, LENGTH_LONG).show()
                uploadSuccessMessage.value = ""
            }
        }
        uploadErrorMessage.observe(viewLifecycleOwner) {
            if (it != "") {
                Snackbar.make(view, it, LENGTH_LONG).show()
                uploadErrorMessage.value = ""
            }
        }
    }

    /**
     * Handles swipe-to-delete
     */
    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val trip = tripsAdapter.differ.currentList[position]
            tripViewModel.deleteTrip(trip)
            Snackbar.make(requireView(), "Successfully deleted run", Snackbar.LENGTH_LONG).apply {
                setAction("Undo") {
                    tripViewModel.insertTrip(trip)
                }
                show()
            }
        }
    }

    private fun setupRecyclerView() = binding.rvTrips.apply {
        adapter = tripsAdapter
        layoutManager = LinearLayoutManager(activity)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(this)
    }

}