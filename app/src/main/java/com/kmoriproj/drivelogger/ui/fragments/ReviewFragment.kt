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
import com.kmoriproj.drivelogger.common.Constants
import com.kmoriproj.drivelogger.common.Constants.Companion.ARGKEY_CURRENT_TRIPID
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR1
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_WIDTH
import com.kmoriproj.drivelogger.common.DateTimeString
import com.kmoriproj.drivelogger.common.RichPoint
import com.kmoriproj.drivelogger.databinding.ReviewFragmentBinding
import com.kmoriproj.drivelogger.ui.ReviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ReviewFragment : Fragment(R.layout.review_fragment),
    OnMapReadyCallback {

    private val reviewViewModel: ReviewViewModel by viewModels()

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
        reviewViewModel.points.observe(viewLifecycleOwner) {
            zoomToWholeTrack(it)
            addAllPoints(it)
        }
        reviewViewModel.currentTrip.observe(viewLifecycleOwner) {
            val caption = it.caption
            val dist = "%.1fkm".format(it.distanceFromStart / 1000)
            val startDt = DateTimeString.formatDate(it.startTime)
            if (caption == "") {
                binding.tvTitle.text = "%s (%s)".format(startDt, dist)
            } else {
                binding.tvTitle.text = "%s (%s)".format(caption, dist)
            }
        }
    }

    private fun addAllPoints(pathPoints: List<RichPoint>) : Int {
        var lastPointIx = 0
        // only add polyline if we have at least two elements in the last polyline
        if (pathPoints.isNotEmpty()) {
            Timber.d("OvO addLatestPolyline size=${pathPoints.size} ix=$lastPointIx")
            if (lastPointIx >= pathPoints.size) {
                Timber.d("    OvO cleared $lastPointIx > $pathPoints.size")
                lastPointIx = 0
                Timber.d("OvO reset")
            }
            var lastPos = pathPoints[lastPointIx++].latlng
            while (lastPointIx < pathPoints.size) {
                val listPts = mutableListOf<LatLng>()
                val color = pathPoints[lastPointIx].color
                var debugstr = ""
                debugstr += "${lastPointIx-1}, "
                listPts.add(lastPos)
                while (lastPointIx < pathPoints.size && color == pathPoints[lastPointIx].color) {
                    lastPos = pathPoints[lastPointIx++].latlng
                    debugstr += "${lastPointIx-1}, "
                    listPts.add(lastPos)
                }
                val colorstr = if (color == POLYLINE_COLOR1) "RED" else if (color == Constants.POLYLINE_COLOR2) "yellow" else "blue"
                Timber.d("OvO $colorstr $debugstr")
                PolylineOptions()
                    .color(color)
                    .width(POLYLINE_WIDTH)
                    .addAll(listPts)
                    .also {
                        mMap.addPolyline(it)
                    }
            }
            lastPointIx--
        }
        return lastPointIx
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
        reviewViewModel.getTrip(tripId!!, viewLifecycleOwner)
    }
    /**
     * Zooms out until the whole track is visible. Used to make a screenshot of the
     * MapView to save it in the database
     */
    private fun zoomToWholeTrack(pts: List<RichPoint>) {
        var found = false
        val bounds = LatLngBounds.Builder()
        for (point in pts) {
            bounds.include(point.latlng)
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

