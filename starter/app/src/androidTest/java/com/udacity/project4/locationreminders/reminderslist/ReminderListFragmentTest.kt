package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.KoinTestRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest {

    private lateinit var appContext: Application
    private lateinit var repository: ReminderDataSource

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val testModule = module {
        viewModel {
            RemindersListViewModel(
                get(),
                get() as ReminderDataSource
            )
        }

        single {
            SaveReminderViewModel(
                get(),
                get() as ReminderDataSource
            )
        }

        single <ReminderDataSource> { RemindersLocalRepository(get()) }
        single { RemindersLocalRepository(get()) }
        single { LocalDB.createRemindersDao(appContext.applicationContext) }
    }

    // In your test class
    @get:Rule
    val koinTestRule = KoinTestRule(
        modules = listOf(testModule)
    )

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)

        // Get our real repository
        repository = GlobalContext.get().get()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)

        // clear the data to start fresh
        runTest {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun clickFAB_navigatesToSaveReminder() {
        // GIVEN - on ReminderList
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        scenario.onFragment {
            // Set the graph on the TestNavHostController
            navController.setGraph(R.navigation.nav_graph)

            // Make the NavController available via the findNavController() APIs
            Navigation.setViewNavController(it.requireView(), navController)
        }

        // WHEN - click on addReminderFAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN verify that we navigate to SaveReminder
        assertThat(navController.currentDestination?.id, `is`(R.id.saveReminderFragment))
    }

    @Test
    fun displayBothRemindersWhenInDatabase() {
        runTest {
            repository.saveReminder(ReminderDTO("Title1", "Description1", "Location1", 1.0, 2.0))
            repository.saveReminder(ReminderDTO("Title2", "Description2", "Location2", 5.0, 9.0))
        }

        // GIVEN - on ReminderList with two Reminders
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        // THEN - UI shows both Reminders
        onView(withText("Title1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(withText("Title1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun displayEmptyResultWhenListCleared() {
        runTest {
            repository.saveReminder(ReminderDTO("Title1", "Description1", "Location1", 1.0, 2.0))
            repository.saveReminder(ReminderDTO("Title2", "Description2", "Location2", 5.0, 9.0))
        }

        // GIVEN - on ReminderList with two Reminders
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        openActionBarOverflowOrOptionsMenu(appContext.applicationContext)

        // THEN - UI shows both Reminders
        onView(withText("Clear list")).perform(click())

        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun displayNoDataViewWhenDatabaseEmpty() {
        runTest {
            repository.deleteAllReminders()
        }

        // GIVEN - ReminderList without Reminders
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        // THEN - UI shows noDataSymbol
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}