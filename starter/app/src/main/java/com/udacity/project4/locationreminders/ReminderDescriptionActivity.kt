package com.udacity.project4.locationreminders

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityReminderDescriptionBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var reminderDataItem: ReminderDataItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        mapView = binding.detailsMapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        @Suppress("DEPRECATION")
        reminderDataItem = intent.extras?.getParcelable(Constants.EXTRA_REMINDER_DATA_ITEM)!!
        binding.reminderDataItem = reminderDataItem
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        val latLng = LatLng(reminderDataItem.latitude!!, reminderDataItem.longitude!!)
        googleMap.addMarker(MarkerOptions().position(latLng).title(reminderDataItem.location))

        // Move the camera to the marker location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        googleMap.uiSettings.setAllGesturesEnabled(false)
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
