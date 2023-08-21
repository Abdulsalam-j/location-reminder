package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest {

    private lateinit var appContext: Application

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

        single<ReminderDataSource> { RemindersLocalRepository(get()) }
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
        appContext =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun givenEmptyReminderTitle_whenSavingReminder_thenShowSnackbarError() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        val snackBarMessage = appContext.getString(R.string.error_enter_title)
        onView(ViewMatchers.withText(snackBarMessage))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun givenReminderTitle_whenSavingReminderWithoutLocation_thenShowSnackbarError() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Title"))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        val snackBarMessage = appContext.getString(R.string.error_select_location)
        onView(ViewMatchers.withText(snackBarMessage))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun givenReminderDetails_whenSavingReminderWithLocation_thenShowToastMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Title"))
        closeSoftKeyboard()
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("Description"))
        closeSoftKeyboard()

        onView(withId(R.id.selectLocation)).perform(ViewActions.click())
        onView(withId(R.id.mapView)).perform(ViewActions.longClick())
        onView(withId(R.id.saveButton)).perform(ViewActions.click())

        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        onView(ViewMatchers.withText(R.string.reminder_saved)).inRoot(
            RootMatchers.withDecorView(
                CoreMatchers.not(
                    CoreMatchers.`is`(
                        getActivity(activityScenario).window.decorView
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}