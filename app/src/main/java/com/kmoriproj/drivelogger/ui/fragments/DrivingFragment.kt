package com.kmoriproj.drivelogger.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants.Companion.ACTION_INIT_LOCATION
import com.kmoriproj.drivelogger.common.Constants.Companion.ACTION_PAUSE_SERVICE
import com.kmoriproj.drivelogger.common.Constants.Companion.ACTION_START_OR_RESUME_SERVICE
import com.kmoriproj.drivelogger.common.Constants.Companion.ACTION_STOP_SERVICE
import com.kmoriproj.drivelogger.common.Constants.Companion.BUNDLE_KEY_MAPVIEW
import com.kmoriproj.drivelogger.common.Constants.Companion.BUNDLE_KEY_POINT_IX
import com.kmoriproj.drivelogger.common.Constants.Companion.MAP_ZOOM
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_COLOR
import com.kmoriproj.drivelogger.common.Constants.Companion.POLYLINE_WIDTH
import com.kmoriproj.drivelogger.common.Constants.Companion.REQUEST_CODE_LOCATION_PERMISSION
import com.kmoriproj.drivelogger.common.TrackingUtility
import com.kmoriproj.drivelogger.databinding.DrivingFragmentBinding
import com.kmoriproj.drivelogger.services.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber


@AndroidEntryPoint
class DrivingFragment : Fragment(R.layout.driving_fragment),
    EasyPermissions.PermissionCallbacks, OnMapReadyCallback {

    private var isTracking = false
    private var currentTimeInMillis = 0L
    private lateinit var binding: DrivingFragmentBinding
    private var pathPoints = mutableListOf<LatLng>()
    private var lastPointIx = 0
    //private val viewModel: MainViewModel by viewModels()

    private lateinit var mMap: GoogleMap
    // *** IMPORTANT ***
    // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
    // objects or sub-Bundles.
    private var mapViewBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(BUNDLE_KEY_MAPVIEW)
            lastPointIx = savedInstanceState.getInt(BUNDLE_KEY_POINT_IX, 0)
        }
    }

    private val mapView
        get() = binding.mapView

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_KEY_POINT_IX, lastPointIx)
        mapViewBundle = outState.getBundle(BUNDLE_KEY_MAPVIEW)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(BUNDLE_KEY_MAPVIEW, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle!!)
    }

    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DrivingFragmentBinding.bind(view)
        binding.mapView.onCreate(mapViewBundle)
        binding.mapView.getMapAsync(this)
        binding.buttonStartStop.setOnClickListener {
            toggleRun()
        }
        binding.buttonTerminate.setOnClickListener {
            stopTrackingService()
            findNavController().navigate(R.id.action_drivingFragment_to_endOfTripFragment)
        }
        requestPermissions()
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = ACTION_INIT_LOCATION
            requireContext().startService(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miTripList -> {
                findNavController().navigate(R.id.action_drivingFragment_to_tripsFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_menu_tracking, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    private fun requestPermissions() {
        if (TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permission to use this app",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
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
        mMap = googleMap

        val uiSettings = mMap.uiSettings
        mMap.mapType = MAP_TYPE_NORMAL
        uiSettings.isZoomGesturesEnabled = true
        uiSettings.isZoomControlsEnabled = true

        TrackingService.isTracking.observe(viewLifecycleOwner) {
            updateTracking(it, TrackingService.isTraveling.value ?: false)
        }

        TrackingService.isTraveling.observe(viewLifecycleOwner) {
            if (it == false) {
                mMap.clear()
            }
        }

        TrackingService.pathPoints.observe(viewLifecycleOwner) {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser(false)
        }

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner) {
            currentTimeInMillis = it as Long
            val timeInSec = currentTimeInMillis / 1000
            binding.tvElapsed.text = "%02d:%02d:%02d".format(
                timeInSec / 3600,
                (timeInSec / 60) % 60,
                timeInSec % 60)
        }

        TrackingService.distanceFromStartKm.observe(viewLifecycleOwner) {
            binding.tvDistance.text = "%dkm".format(it.toInt())
        }

        TrackingService.initLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                val initPos = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initPos, MAP_ZOOM))
            }
        }
    }
    /**
     * Will move the camera to the user's location.
     */
    private fun moveCameraToUser(zoom: Boolean=false) {
        if (pathPoints.isNotEmpty()) {
            mMap.animateCamera(
                if (zoom) {
                    CameraUpdateFactory.newLatLngZoom(
                        pathPoints.last(),
                        MAP_ZOOM
                    )
                } else {
                    CameraUpdateFactory.newLatLng(
                        pathPoints.last(),
                    )
                }
            )
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).setThemeResId(R.style.AlertDialogTheme).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
    /**
     * Draws a polyline between the two latest points.
     */
    private fun addLatestPolyline() {
        // only add polyline if we have at least two elements in the last polyline
        if (pathPoints.isNotEmpty()) {
            Timber.d("OvO addLatestPolyline size=${pathPoints.size} ix=${lastPointIx}")
            if (lastPointIx >= pathPoints.size) {
                Timber.d("    OvO cleared ${lastPointIx} > ${pathPoints.size}")
                lastPointIx = 0
                Timber.d("OvO reset")
            }
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
            for (j in lastPointIx .. pathPoints.size - 1) {
                polylineOptions.add(pathPoints[j])
                lastPointIx = j
            }
            mMap.addPolyline(polylineOptions)
        }
    }

    /**
     * Updates the tracking variable and the UI accordingly
     */
    private fun updateTracking(isTracking: Boolean, isTraveling: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            if (!isTraveling) {
                binding.buttonStartStop.text = getString(R.string.start_text)
                binding.buttonTerminate.visibility = View.GONE
            } else {
                binding.buttonStartStop.text = getString(R.string.restart_text)
                binding.buttonTerminate.visibility = View.VISIBLE
            }
        } else if (isTracking) {
            binding.buttonStartStop.text = getString(R.string.pause_text)
            binding.buttonTerminate.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun toggleRun() {
        if (isTracking) {
            pauseTrackingService()
        } else {
            startOrResumeTrackingService()
        }
    }
    /**
     * Starts the tracking service or resumes it if it is currently paused.
     */
    private fun startOrResumeTrackingService() =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE
            requireContext().startService(it)
        }

    /**
     * Pauses the tracking service
     */
    private fun pauseTrackingService() =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = ACTION_PAUSE_SERVICE
            requireContext().startService(it)
        }

    /**
     * Stops the tracking service.
     */
    private fun stopTrackingService() =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            requireContext().startService(it)
        }
}