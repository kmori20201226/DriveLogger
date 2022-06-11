package com.kmoriproj.drivelogger.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.adapters.TripsAdapter
import com.kmoriproj.drivelogger.common.Constants.Companion.ARGKEY_CURRENT_TRIPID
import com.kmoriproj.drivelogger.common.SortType
import com.kmoriproj.drivelogger.databinding.FragmentTripsBinding
import com.kmoriproj.drivelogger.ui.TrajectoryViewModel
import com.kmoriproj.drivelogger.ui.TripViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripsFragment : Fragment(R.layout.fragment_trips) {
    lateinit var tripsAdapter: TripsAdapter

    private val tripViewModel: TripViewModel by viewModels()

    private lateinit var binding: FragmentTripsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel = (activity as MainActivity).mainViewModel
        tripsAdapter = TripsAdapter {
            val bundle = Bundle()
            bundle.putLong(ARGKEY_CURRENT_TRIPID, it.id!!)
            findNavController().navigate(R.id.action_tripsFragment_to_reviewFragment,bundle)
        }
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