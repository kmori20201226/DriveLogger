package com.kmoriproj.drivelogger.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.databinding.ActivityMainBinding
import com.kmoriproj.drivelogger.ui.fragments.DrivingFragment

class MapsActivity : AppCompatActivity() {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        // setContentView(R.layout.activity_main)
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.navHostFragment, DrivingFragment.newInstance())
//                .commitNow()
//        }

//
//        setSupportActionBar(binding.toolbar)
//        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
//        binding.bottomNavigationView.setOnNavigationItemReselectedListener { /* NO-OP */ }
//
//        navigateToTrackingFragmentIfNeeded(intent)
//
//        navHostFragment.findNavController()
//            .addOnDestinationChangedListener { _, destination, _ ->
//                when (destination.id) {
//                    R.id.setupFragment2, R.id.trackingFragment -> bottomNavigationView.visibility =
//                        View.GONE
//                    else -> bottomNavigationView.visibility = View.VISIBLE
//                }
//            }
    }

}