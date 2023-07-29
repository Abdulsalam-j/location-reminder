package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeReminderDataSource : ReminderDataSource {

    private val reminders = mutableMapOf<String, ReminderDTO>()

    private var shouldReturnsError = false

    fun setShouldReturnsError(flag: Boolean) {
        shouldReturnsError = flag
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnsError) return Result.Error("Error")

        return Result.Success(reminders.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders[reminder.id] = reminder
    }

    override suspend fun getReminderById(id: String): Result<ReminderDTO> {
        if (shouldReturnsError) return Result.Error("Error")

        val reminder = reminders[id]
        return if (reminder != null) {
            Result.Success(reminder)
        } else {
            Result.Error("Reminder not found!")
        }
    }

    override suspend fun deleteReminderById(id: String) {
        reminders.remove(id)
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}