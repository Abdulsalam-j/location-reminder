package com.udacity.project4.locationreminders.reminderslist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * data class acts as a data mapper between the DB and the UI
 */
@Parcelize
data class ReminderDataItem(
    val title: String?,
    val description: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val id: String = UUID.randomUUID().toString()
) : Parcelable