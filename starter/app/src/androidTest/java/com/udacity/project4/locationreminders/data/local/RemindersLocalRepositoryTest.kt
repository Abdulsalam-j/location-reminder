package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // Using an in-memory database for testing, with a custom name
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminderAndGetById() = runTest {
        // Create a sample reminder
        val reminder =
            ReminderDTO("Sample Title", "Sample Description", "Sample Location", 10.0, 20.0)

        // Save the reminder to the repository
        repository.saveReminder(reminder)

        // Get the reminder by its ID from the repository
        val result = repository.getReminderById(reminder.id)

        // Ensure the result is of type Result.Success and contains the same reminder
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getNonExistentReminderById() = runTest {
        // Try to get a reminder by a non-existent ID from the repository
        val result = repository.getReminderById("non_existent_id")

        // Ensure the result is of type Result.Error with the appropriate error message
        assertThat(result, instanceOf(Result.Error::class.java))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun getReminders_whenRemindersExist() = runTest {
        // Save multiple reminders to the repository
        val reminder1 = ReminderDTO("Title 1", "Description 1","Sample Location1",  10.0, 20.0)
        val reminder2 = ReminderDTO("Title 2", "Description 2", "Sample Location2", 30.0, 40.0)
        val reminder3 = ReminderDTO("Title 3", "Description 3", "Sample Location3", 50.0, 60.0)

        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // Get all the reminders from the repository
        val result = repository.getReminders()

        // Ensure the result is of type Result.Success and contains all the reminders
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.size, `is`(3))
        assertThat(result.data, containsInAnyOrder(reminder1, reminder2, reminder3))
    }

    @Test
    fun getReminders_whenNoRemindersExist() = runTest {
        // Try to get reminders when no reminders are saved in the repository
        val result = repository.getReminders()

        // Ensure the result is of type Result.Success and contains an empty list
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun deleteReminderById_whenReminderExists() = runTest {
        // Save a reminder to the repository
        val reminder = ReminderDTO("Sample Title", "Sample Description", "Sample Location", 10.0, 20.0)
        repository.saveReminder(reminder)

        // Delete the reminder by its ID from the repository
        repository.deleteReminderById(reminder.id)

        // Try to get the deleted reminder by its ID
        val getResult = repository.getReminderById(reminder.id)

        // Ensure the result is of type Result.Error with the appropriate error message
        assertThat(getResult, instanceOf(Result.Error::class.java))
        getResult as Result.Error
        assertThat(getResult.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteReminderById_whenReminderDoesNotExist() = runTest {
        // Try to delete a non-existent reminder by its ID from the repository
        repository.deleteReminderById("non_existent_id")

        // No exceptions should be thrown if the reminder does not exist
        // The repository should handle this gracefully
    }

    @Test
    fun deleteAllReminders() = runTest {
        // Save multiple reminders to the repository
        val reminder1 = ReminderDTO("Title 1", "Description 1", "Sample Location1", 10.0, 20.0)
        val reminder2 = ReminderDTO("Title 2", "Description 2", "Sample Location2", 30.0, 40.0)
        val reminder3 = ReminderDTO("Title 3", "Description 3", "Sample Location3", 50.0, 60.0)

        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // Delete all the reminders from the repository
        repository.deleteAllReminders()

        // Get all the reminders from the repository
        val result = repository.getReminders()

        // Ensure the result is of type Result.Success and contains an empty list
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }
}