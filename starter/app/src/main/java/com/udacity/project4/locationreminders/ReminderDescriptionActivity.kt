package com.udacity.project4.locationreminders

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity(), OnMapReadyCallback {

    private val dataSource: ReminderDataSource = get()

    private lateinit var binding: ActivityReminderDescriptionBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        geofencingClient = LocationServices.getGeofencingClient(this)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mapView = binding.detailsMapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        @Suppress("DEPRECATION")
        reminderDataItem = intent.extras?.getParcelable(Constants.EXTRA_REMINDER_DATA_ITEM)!!
        binding.reminderDataItem = reminderDataItem

        binding.deleteButton.setOnClickListener {
            val geofenceIds = listOf(reminderDataItem.id)

            geofencingClient.removeGeofences(
                geofenceIds
            ).addOnSuccessListener {
                lifecycleScope.launch {
                    runCatching {
                        dataSource.deleteReminderById(geofenceIds.first())
                    }.onSuccess {
                        finish()
                    }.onFailure {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.default_error_message), Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }.addOnCanceledListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.operation_cancelled), Snackbar.LENGTH_SHORT
                ).show()
            }.addOnFailureListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.err_removing_geofences), Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        val latLng = LatLng(reminderDataItem.latitude!!, reminderDataItem.longitude!!)
        googleMap.addMarker(MarkerOptions().position(latLng).title(reminderDataItem.location))

        // Move the camera to the marker location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        googleMap.uiSettings.setAllGesturesEnabled(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
