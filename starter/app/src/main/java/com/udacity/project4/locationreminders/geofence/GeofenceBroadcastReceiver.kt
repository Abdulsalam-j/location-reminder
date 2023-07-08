package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendReminderNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import kotlin.coroutines.CoroutineContext

private const val TAG = "GeofenceBroadcastReceiver"

/**
 * Triggered by the Geofence request. Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background, to do that we use work manager.
 *
 */
class GeofenceBroadcastReceiver : BroadcastReceiver(), CoroutineScope {
    // Get the local repository instance
    private val remindersLocalDataSource: ReminderDataSource by inject(ReminderDataSource::class.java)

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent?.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Send notification
            triggeringGeofences?.let { sendNotification(context, it) } ?: Log.e(
                TAG,
                context.getString(R.string.triggering_geofences_is_null)
            )
        } else {
            Log.e(
                TAG,
                context.getString(
                    R.string.geofence_transition_enter_is_not_received,
                    geofenceTransition
                )
            )
        }
    }

    private fun sendNotification(context: Context, triggeringGeofences: List<Geofence>) {
        for (i in triggeringGeofences.indices) {
            val requestId = triggeringGeofences[i].requestId

            // Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                // get the reminder with the request id
                val result = remindersLocalDataSource.getReminderById(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    val reminderDataItem = ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.id
                    )

                    // send a notification to the user with the reminder details
                    sendReminderNotification(
                        context,
                        reminderDataItem
                    )
                }
            }
        }
    }
}