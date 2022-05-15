package com.kmoriproj.drivelogger.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.common.Constants.Companion.MAPVIEW_BUNDLE_KEY
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class DrivingFragment : Fragment(R.layout.driving_fragment),
    EasyPermissions.PermissionCallbacks, OnMapReadyCallback {

    companion object {
        fun newInstance() = DrivingFragment()
    }

    private lateinit var mMap: GoogleMap
    // *** IMPORTANT ***
    // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
    // objects or sub-Bundles.
    private var mapViewBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        val mMapView = view?.findViewById(R.id.mapView) as MapView
        mMapView.onSaveInstanceState(mapViewBundle!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mMapView = view.findViewById(R.id.mapView) as MapView
        mMapView.onCreate(mapViewBundle)
        mMapView.getMapAsync(this)
    }

    override fun onDestroy() {
        val mMapView = view?.findViewById(R.id.mapView) as MapView
        mMapView.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        val mMapView = view?.findViewById(R.id.mapView) as MapView
        super.onResume()
        mMapView.onResume()
    }

    override fun onPause() {
        val mMapView = view?.findViewById(R.id.mapView) as MapView
        super.onPause()
        mMapView.onPause()
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

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        val uiSettings = mMap.uiSettings
        mMap.mapType = MAP_TYPE_NORMAL
        uiSettings.isZoomGesturesEnabled = true
        uiSettings.isZoomControlsEnabled = true
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }
}