package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.empty
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.S])
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var fakeDataSource: FakeReminderDataSource
    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        Dispatchers.setMain(StandardTestDispatcher())
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeDataSource = FakeReminderDataSource()
        viewModel = RemindersListViewModel(application, fakeDataSource)
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.2)
        runTest {
            fakeDataSource.saveReminder(reminder)
        }
    }

    @After
    fun tearDown() {
        runTest {
            fakeDataSource.deleteAllReminders()
        }
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `when loadReminders() is called then loading start`() = runTest {
        viewModel.loadReminders()
        assertThat(
            viewModel.showLoading.getOrAwaitValue(), Matchers.`is`(true)
        )

        advanceUntilIdle()

        assertThat(
            viewModel.showLoading.getOrAwaitValue(), Matchers.`is`(false)
        )
    }

    @Test
    fun `when clearList() is called then list is cleared`() = runTest {
        assertThat(
            viewModel.remindersList.value, Matchers.not(empty())
        )
        viewModel.clearList()

        advanceUntilIdle()

        assertThat(
            viewModel.remindersList.value, Matchers.`is`(empty())
        )
    }

    @Test
    fun `when error happens should return error`() = runTest {
        fakeDataSource.setShouldReturnsError(true)
        viewModel.loadReminders()

        advanceUntilIdle()

        assertThat(
            viewModel.showSnackBar.getOrAwaitValue(),
            Matchers.`is`(Matchers.notNullValue())
        )
    }
}