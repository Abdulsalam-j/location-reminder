package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
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

            val reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)

            val isReminderDataValid = viewModel.validateReminderData(reminderDataItem)

            if (isReminderDataValid) {
                addGeofenceRequest(reminderDataItem)
            } else {
                Snackbar.make(
                    binding.root,
                    getString(viewModel.showSnackBarResId.value ?: R.string.default_error_message),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceRequest(reminderDataItem: ReminderDataItem) {
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
                viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
            addOnCanceledListener {
                Snackbar.make(binding.root,
                    getString(R.string.operation_cancelled), Snackbar.LENGTH_SHORT).show()
                viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
            addOnFailureListener {
                Snackbar.make(binding.root,
                    getString(R.string.err_adding_geofence), Snackbar.LENGTH_SHORT).show()
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
