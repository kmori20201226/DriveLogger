package com.kmoriproj.drivelogger.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants.Companion.ARGKEY_CURRENT_TRIPID
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_WIDTH
import com.kmoriproj.drivelogger.databinding.ReviewFragmentBinding
import com.kmoriproj.drivelogger.ui.TrajectoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewFragment : Fragment(R.layout.review_fragment),
    OnMapReadyCallback {

    private val trajectoryViewModel: TrajectoryViewModel by viewModels()

    private lateinit var binding: ReviewFragmentBinding

    private lateinit var mMap: GoogleMap
    // *** IMPORTANT ***
    // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
    // objects or sub-Bundles.
    private var mapViewBundle: Bundle? = null

    private val mapView
        get() = binding.mapView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ReviewFragmentBinding.bind(view)
        binding.mapView.onCreate(mapViewBundle)
        binding.mapView.getMapAsync(this)
        trajectoryViewModel.points.observe(viewLifecycleOwner) {
            zoomToWholeTrack(it)
            addAllPoints(it)
        }
    }

    private fun addAllPoints(pts: List<LatLng>) {
        val polylineOptions = PolylineOptions()
            .color(POLYLINE_COLOR)
            .width(POLYLINE_WIDTH)
            .addAll(pts)
        mMap.addPolyline(polylineOptions)
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val uiSettings = mMap.uiSettings
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        uiSettings.isZoomGesturesEnabled = true
        uiSettings.isZoomControlsEnabled = true

        val tripId = arguments?.getLong(ARGKEY_CURRENT_TRIPID)
        trajectoryViewModel.trajectoryOf(tripId!!)
    }
    /**
     * Zooms out until the whole track is visible. Used to make a screenshot of the
     * MapView to save it in the database
     */
    private fun zoomToWholeTrack(pts: List<LatLng>) {
        var found = false
        val bounds = LatLngBounds.Builder()
        for (point in pts) {
            bounds.include(point)
            found = true
        }
        val width = mapView.width
        val height = mapView.height
        if (found) {
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds.build(),
                    width,
                    height,
                    (height * 0.05f).toInt()
                )
            )
        }
    }
}

