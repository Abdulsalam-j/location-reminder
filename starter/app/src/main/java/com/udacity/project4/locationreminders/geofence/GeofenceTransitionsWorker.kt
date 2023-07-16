package com.udacity.project4.locationreminders.geofence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendReminderNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceTransitionsWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
         fun enqueueGeofenceWorker(context: Context, triggeringGeofences: List<Geofence>) {
            for (i in triggeringGeofences.indices) {
                val requestId = triggeringGeofences[i].requestId

                // Create a work data object to pass any data needed by the worker
                val inputData = workDataOf("requestId" to requestId)

                // Create a work request
                val workRequest = OneTimeWorkRequestBuilder<GeofenceTransitionsWorker>()
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                    .build()

                // Enqueue the work request
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    // Retrieve the local repository instance
    private val remindersLocalRepository: ReminderDataSource by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val requestId = inputData.getString("requestId")

        if (requestId.isNullOrEmpty()) {
            return@withContext Result.failure()
        }

        // Fetch the reminder with the request id
        val result = remindersLocalRepository.getReminderById(requestId)
        if (result is Success) {
            val reminderDTO = result.data
            // Send a notification to the user with the reminder details
            sendNotification(
                appContext,
                ReminderDataItem(
                    reminderDTO.title,
                    reminderDTO.description,
                    reminderDTO.location,
                    reminderDTO.latitude,
                    reminderDTO.longitude,
                    reminderDTO.id
                )
            )
            return@withContext Result.success()
        }
        Result.failure()
    }

    private fun sendNotification(context: Context, reminderDataItem: ReminderDataItem) {
        // send a notification to the user with the reminder details
        sendReminderNotification(
            context,
            reminderDataItem
        )
    }
}