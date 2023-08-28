package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    companion object {
        const val GEOFENCE_RADIUS_IN_METERS = 100f
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val viewModel: SaveReminderViewModel by inject()

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminderDataItem: ReminderDataItem

    private val deviceLocationActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User clicked "OK" on the resolution dialog
                addGeofenceRequest()
            } else {
                // User clicked "No Thanks" on the resolution dialog
                showEnableLocationDialog()
            }
        }

    private val locationResolutionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            addGeofenceRequest()
        }

    private val locationPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) addGeofenceRequest()
        else showPermissionDeniedDialog()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_dialog_title)
            .setMessage(getString(R.string.dialogue_grant_location_permission))
            .setPositiveButton(R.string.location_permission_dialog_positive_button) { _: DialogInterface, _: Int ->
                requestLocationPermission()
            }
            .setCancelable(false)
            .show()
    }

    private val locationPermissionsRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(
                Manifest.permission.ACCESS_FINE_LOCATION,
                false
            ) && permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false)
        ) {
            addGeofenceRequest()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = viewModel.reminderTitle.value
            val description = viewModel.reminderDescription.value
            val location = viewModel.reminderSelectedLocationString.value
            val latitude = viewModel.latitude.value
            val longitude = viewModel.longitude.value

            reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)

            val isReminderDataValid = viewModel.validateReminderData(reminderDataItem)

            if (isReminderDataValid) {
                if (isLocationPermissionsGranted()) {
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    requestLocationPermission()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    getString(viewModel.showSnackBarResId.value ?: R.string.default_error_message),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isLocationPermissionsGranted(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )

        // If your app targets Android 10 (API level 29) or higher
        val backgroundPermissionApproved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun checkDeviceLocationSettingsAndStartGeofence() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(50)
                .setMaxUpdateDelayMillis(100)
                .build()

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(requireActivity())
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                addGeofenceRequest()
            }

            task.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            deviceLocationActivityResultLauncher.launch(
                                IntentSenderRequest.Builder(
                                    resolvable.resolution
                                ).build()
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Error occurred while trying to start the resolution intent.
                            e.printStackTrace()
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else addGeofenceRequest()
    }

    @TargetApi(29)
    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationPermissionsRequestLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    private fun showEnableLocationDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle(getString(R.string.enable_location))
            .setMessage(getString(R.string.request_location_dialogue_message))
            .setPositiveButton(getString(R.string.enable)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationResolutionLauncher.launch(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceRequest() {
        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(reminderDataItem.id)

            // Set the circular region of this geofence.
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

            // Create the geofence.
            .build()

        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        val geofenceRequest = getGeofencingRequest(listOf(geofence))
        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                viewModel.saveReminder(reminderDataItem)
                viewModel.showToast.postValue(getString(R.string.reminder_saved))
                viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
            addOnCanceledListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.operation_cancelled), Snackbar.LENGTH_SHORT
                ).show()
                viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
            addOnFailureListener {
                Snackbar.make(
                    binding.root,
                    getString(R.string.err_adding_geofence), Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // make sure to clear the view model after destroy, as it's a single view model.
        viewModel.onClear()
    }
}
