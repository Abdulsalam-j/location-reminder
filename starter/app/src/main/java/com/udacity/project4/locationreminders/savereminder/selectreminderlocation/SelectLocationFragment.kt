package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val DEFAULT_ZOOM = 15
        private const val REQUEST_ENABLE_LOCATION = 123
    }

    override val viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var locationResolutionLauncher: ActivityResultLauncher<Intent>


    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var lastKnownLocation: Location? = null

    private val locationPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                zoomToUserLocation()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                showPermissionDeniedDialog("Please grant the precise location permission so you get precise results and reminders")
            }

            else -> {
                showPermissionDeniedDialog("Please grant the location permission so you benefit from the app")
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationResolutionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == REQUEST_ENABLE_LOCATION) {
                    zoomToUserLocation()
                } else {
                    showEnableLocationDialog()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setHasOptionsMenu(true)

        setDisplayHomeAsUpEnabled(true)

        // TODO: add style to the map
        // TODO: put a marker to location that the user selected

        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        /** TODO: When the user confirms on the selected location,
        send back the selected location details to the view model
        and navigate back to the previous fragment to save the reminder and add the geofence
         */
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }

        R.id.hybrid_map -> {
            true
        }

        R.id.satellite_map -> {
            true
        }

        R.id.terrain_map -> {
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableLocationServices(requireContext())
    }

    private fun requestLocationPermission() {
        locationPermissionRequestLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        googleMap.isMyLocationEnabled = true

        try {
            val locationResult = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        val latLng = LatLng(
                            lastKnownLocation!!.latitude,
                            lastKnownLocation!!.longitude
                        )
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                latLng, DEFAULT_ZOOM.toFloat()
                            )
                        )
                    }
                } else {
                    googleMap.moveCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun showPermissionDeniedDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.location_permission_dialog_positive_button) { _: DialogInterface, _: Int ->
                requestLocationPermission()
            }
            .setNegativeButton(R.string.location_permission_dialog_negative_button) { _: DialogInterface, _: Int ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableLocationServices(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(50)
                .setMaxUpdateDelayMillis(150)
                .build()

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(requireActivity())
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                zoomToUserLocation()
            }

            task.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            // Location services are not enabled, but a resolution is available.
                            // Prompt the user to enable location services.
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            locationResolutionLauncher.launch(intent)
                        } catch (e: IntentSender.SendIntentException) {
                            // Error occurred while trying to start the resolution intent.
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun showEnableLocationDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle(getString(R.string.enable_location))
            .setMessage(getString(R.string.show_your_location_dialogue_message))
            .setPositiveButton(getString(R.string.enable)) { dialog: DialogInterface, _: Int ->
                // User clicked Enable
                enableLocationServices(requireContext())
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, _: Int ->
                // User clicked Cancel
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}