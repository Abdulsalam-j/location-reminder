package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.S])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource: FakeReminderDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        Dispatchers.setMain(StandardTestDispatcher())
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeDataSource = FakeReminderDataSource()
        viewModel = SaveReminderViewModel(application, fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `validateEnteredData with valid data returns true`() {
        val reminderData = ReminderDataItem("Title", "Description", "Location", 10.0, 20.0)

        val isValid = viewModel.validateEnteredData(reminderData)

        assertThat(isValid, `is`(true))
    }

    @Test
    fun `validateEnteredData with missing title returns false`() {
        val reminderData = ReminderDataItem("", "Description", "Location", 10.0, 20.0)

        val isValid = viewModel.validateEnteredData(reminderData)

        assertThat(isValid, `is`(false))
        assertThat(viewModel.showSnackBarResId.getOrAwaitValue(), `is`(notNullValue()))
    }

    @Test
    fun `onClear resets LiveData objects`() {
        val reminderData = ReminderDataItem("Title", "Description", "Location", 10.0, 20.0)

        viewModel.saveReminder(reminderData)

        viewModel.onClear()

        assertThat(viewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationString.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }

    @Test
    fun `saveReminder shows loading and saves reminder`() = runTest {
        val reminderData = ReminderDataItem("Title", "Description", "Location", 10.0, 20.0)

        viewModel.saveReminder(reminderData)

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        advanceUntilIdle()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))

        // Verify that the reminder was saved to the FakeDataSource
        val savedReminder = fakeDataSource.getReminderById(reminderData.id)
        assertThat(savedReminder, instanceOf(Result.Success::class.java))
        savedReminder as Result.Success
        assertThat(savedReminder.data.id, `is`(reminderData.id))
        assertThat(savedReminder.data.title, `is`(reminderData.title))
        assertThat(savedReminder.data.description, `is`(reminderData.description))
        assertThat(savedReminder.data.location, `is`(reminderData.location))
        assertThat(savedReminder.data.latitude, `is`(reminderData.latitude))
        assertThat(savedReminder.data.longitude, `is`(reminderData.longitude))
    }
}