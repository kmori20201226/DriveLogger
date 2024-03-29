package com.kmoriproj.drivelogger.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.kmoriproj.drivelogger.BaseApplication
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants.Companion.BUNDLE_KEY_MAPVIEW
import com.kmoriproj.drivelogger.common.Constants.Companion.BUNDLE_KEY_POINT_IX
import com.kmoriproj.drivelogger.common.Constants.Companion.BUNDLE_KEY_SPOT_IX
import com.kmoriproj.drivelogger.common.Constants.Companion.MAP_ZOOM
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR1
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR2
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_WIDTH
import com.kmoriproj.drivelogger.common.Polyline
import com.kmoriproj.drivelogger.databinding.DrivingFragmentBinding
import com.kmoriproj.drivelogger.db.Spot
import com.kmoriproj.drivelogger.ui.viewmodels.DrivingViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

@AndroidEntryPoint
class DrivingFragment : Fragment(R.layout.driving_fragment),
    OnMapReadyCallback {

    private val viewModel: DrivingViewModel by viewModels()

    private var isTravelling = false
    private lateinit var binding: DrivingFragmentBinding
    private var lastSpotIx = 0
    private var lastPointIx = 0

    private lateinit var mMap: GoogleMap

    // *** IMPORTANT ***
    // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
    // objects or sub-Bundles.
    private var mapViewBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        isTravelling = false
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(BUNDLE_KEY_MAPVIEW)
            lastSpotIx = savedInstanceState.getInt(BUNDLE_KEY_SPOT_IX, 0)
            lastPointIx = savedInstanceState.getInt(BUNDLE_KEY_POINT_IX, 0)
        }
        Log.d("OvO", "DrivingFragment onCreate")
        viewModel.initService()
    }

    private val mapView
        get() = binding.mapView

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_KEY_POINT_IX, lastPointIx)
        outState.putInt(BUNDLE_KEY_SPOT_IX, lastSpotIx)
        mapViewBundle = outState.getBundle(BUNDLE_KEY_MAPVIEW)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(BUNDLE_KEY_MAPVIEW, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("OvO", "DrivingFragment onCreateView")

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DrivingFragmentBinding.bind(view)
        binding.toolbar2.inflateMenu(R.menu.toolbar_menu_drivingfragment)
        binding.toolbar2.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.miTripList -> {
                    findNavController().navigate(R.id.action_drivingFragment_to_tripsFragment)
                    true
                }
                R.id.miConfigure -> {
                    findNavController().navigate(R.id.action_global_settingsFragment)
                    true
                }
                else -> false
            }
        }
        binding.mapView.onCreate(mapViewBundle)
        if (foregroundPermissionApproved()) {
            viewModel.getLastLocation().addOnCompleteListener() {
                if (it.result != null) {
                    moveCameraToUser(LatLng(it.result.latitude, it.result.longitude))
                }
            }
        }
        binding.mapView.getMapAsync(this)
        binding.buttonStartStop.setOnClickListener {
            if (viewModel.isTravelling.value != true) {
                viewModel.startTracking()
            } else if (viewModel.isTracking.value != true) {
                viewModel.resumeTracking();
            } else {
                viewModel.pauseTracking()
            }
        }
        binding.buttonTerminate.setOnClickListener {
            //viewModel.endTracking()
            Log.d("OvO", "DrivingFragment goto endOfTripFragment")
            viewModel.endTracking()
            findNavController().navigate(R.id.action_drivingFragment_to_endOfTripFragment)
        }
        requestPermissions()
    }

    override fun onDestroy() {
        Log.d("OvO", "DrivingFragment onDestroy")
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onStart() {
        Log.d("OvO", "DrivingFragment onStart")
        super.onStart()
        updateButtonText()
    }

    override fun onResume() {
        Log.d("OvO", "DrivingFragment onResume")
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        Log.d("OvO", "DrivingFragment onPause")
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        Log.d("OvO", "DrivingFragment onStop")
        viewModel.finishService()
        super.onStop()
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("OvO", "DrivingFragment onMapReady")
        mMap = googleMap

        val uiSettings = mMap.uiSettings
        mMap.mapType = MAP_TYPE_NORMAL
        uiSettings.isZoomGesturesEnabled = true
        uiSettings.isZoomControlsEnabled = true

        viewModel.isTracking.observe(viewLifecycleOwner) {
            updateButtonText()
        }

        lastSpotIx = 0
        lastPointIx = 0

        viewModel.isTravelling.observe(viewLifecycleOwner) {
            if (it == false) {
                mMap.clear()
            }
        }

        viewModel.stillSpots.observe(viewLifecycleOwner) {
            addStillSpot(it)
        }

        viewModel.pathPoints.observe(viewLifecycleOwner) {
            addLatestPolyline(it)
            if (viewModel.isTracking.value == true) {
                moveCameraToUser(it)
            }
        }

        viewModel.timeRunInSeconds.observe(viewLifecycleOwner) {
            timeInSec ->
            binding.tvElapsed.text = "%02d:%02d:%02d".format(
                timeInSec / 3600,
                (timeInSec / 60) % 60,
                timeInSec % 60
            )
        }

        viewModel.distanceFromStartKm.observe(viewLifecycleOwner) {
            binding.tvDistance.text = "%.1fkm".format(it)
        }
    }

    /**
     * Will move the camera to the user's location.
     */
    private fun moveCameraToUser(pathPoints: Polyline) {
        if (pathPoints.isNotEmpty()) {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLng(pathPoints.last().latlng)
            )
        }
    }

    private fun moveCameraToUser(latlng: LatLng) {
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latlng, MAP_ZOOM)
        )
    }
    /**
     * Updates the tracking variable and the UI accordingly
     */
    private fun updateButtonText() {
        var showTripList = false
        if (viewModel.isTracking.value == true) {
            binding.buttonStartStop.text = getString(R.string.pause_text)
            binding.buttonTerminate.visibility = View.GONE
        } else {
            if (viewModel.isTravelling.value == true) {
                binding.buttonStartStop.text = getString(R.string.resume_text)
                binding.buttonTerminate.visibility = View.VISIBLE
            } else {
                showTripList = true
                binding.buttonStartStop.text = getString(R.string.start_text)
                binding.buttonTerminate.visibility = View.GONE
            }
        }
        binding.toolbar2.menu.findItem(R.id.miTripList).setVisible(showTripList)
    }
    private fun addStillSpot(stillSpots: List<Spot>) {
        fun drawSpot(spot: Spot) {
            mMap.addMarker(
                MarkerOptions().position(spot.point)
            )
        }
        if (stillSpots.isNotEmpty()) {
            if (lastSpotIx >= stillSpots.size) {
                lastSpotIx = 0
            }
            while (lastSpotIx < stillSpots.size) {
                drawSpot(stillSpots[lastSpotIx++])
            }
        }
    }
    /**
     * Draws a polyline between the two latest points.
     */
    private fun addLatestPolyline(pathPoints: Polyline) {
        // only add polyline if we have at least two elements in the last polyline
        if (pathPoints.isNotEmpty()) {
            Log.d("OvO", "OvO addLatestPolyline size=${pathPoints.size} ix=${lastPointIx}")
            if (lastPointIx >= pathPoints.size) {
                Log.d("OvO", "    OvO cleared ${lastPointIx} > $pathPoints.size")
                lastPointIx = 0
                Log.d("OvO", "OvO reset")
            }
            var lastPos = pathPoints[lastPointIx++].latlng
            while (lastPointIx < pathPoints.size) {
                val listPts = mutableListOf<LatLng>()
                var color = pathPoints[lastPointIx].color
                var debugstr = ""
                debugstr += "${lastPointIx-1}, "
                listPts.add(lastPos)
                while (lastPointIx < pathPoints.size && color == pathPoints[lastPointIx].color) {
                    lastPos = pathPoints[lastPointIx++].latlng
                    debugstr += "${lastPointIx-1}, "
                    listPts.add(lastPos)
                }
                val colorstr = if (color == POLYLINE_COLOR1) "RED" else if (color == POLYLINE_COLOR2) "yellow" else "blue"
                Log.d("OvO", "OvO ${colorstr} ${debugstr}")
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
    }

    /// MIGRATED

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {

            Snackbar.make(
                view?.rootView!!,
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        activity!!,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }
}

