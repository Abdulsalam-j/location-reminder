package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var remindersDao: RemindersDao

    @Before
    fun setup() {
        // Create an in-memory version of the database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersDao = database.reminderDao()
    }

    @After
    fun cleanUp() {
        // Close the in-memory database after each test
        database.close()
    }

    @Test
    fun getReminders_emptyDatabase() = runTest {
        // Get all reminders from the empty database
        val reminders = remindersDao.getReminders()

        // Ensure the list is empty
        assertThat(reminders.isEmpty(), `is`(true))
    }

    @Test
    fun saveReminder_getReminderById() = runTest {
        // Create a sample ReminderDTO
        val reminder = ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            1.2345,
            2.3456
        )

        // Save the reminder to the database
        remindersDao.saveReminder(reminder)

        // Get the reminder by its ID from the database
        val loadedReminder = remindersDao.getReminderById(reminder.id)

        // Ensure the loaded reminder is not null
        assertThat(loadedReminder, notNullValue())

        // Ensure the loaded reminder's properties match the original reminder
        assertThat(loadedReminder?.id, `is`(reminder.id))
        assertThat(loadedReminder?.title, `is`("Test Title"))
        assertThat(loadedReminder?.description, `is`("Test Description"))
        assertThat(loadedReminder?.location, `is`("Test Location"))
        assertThat(loadedReminder?.latitude, `is`(1.2345))
        assertThat(loadedReminder?.longitude, `is`(2.3456))
    }

    @Test
    fun deleteReminderById_reminderIsDeleted() = runTest {
        // Create a sample ReminderDTO
        val reminder = ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            1.2345,
            2.3456
        )

        // Save the reminder to the database
        remindersDao.saveReminder(reminder)

        // Delete the reminder by its ID from the database
        remindersDao.deleteReminderById(reminder.id)

        // Get the reminder by its ID from the database
        val loadedReminder = remindersDao.getReminderById(reminder.id)

        // Ensure the loaded reminder is null, indicating it was deleted
        assertThat(loadedReminder, `is`(nullValue()))
    }

    @Test
    fun deleteAllReminders_databaseIsEmpty() = runTest {
        // Create a sample ReminderDTO
        val reminder = ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            1.2345,
            2.3456
        )

        // Save the reminder to the database
        remindersDao.saveReminder(reminder)

        // Delete all reminders from the database
        remindersDao.deleteAllReminders()

        // Get all reminders from the database
        val reminders = remindersDao.getReminders()

        // Ensure the list is empty
        assertThat(reminders.isEmpty(), `is`(true))
    }
}