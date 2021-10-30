package org.dark0ghost.android_screen_recorder

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.dark0ghost.android_screen_recorder", appContext.packageName)
    }

    @Test
    fun clickStartInlineButton() {
        onView(
            withId(R.id.start_inline_button)
        )
            .perform(
                click()
            )
            .check(
                matches(isClickable())
            )
    }

    fun clickStartRecordButton(){
        onView(
            withId(R.id.start_record)
        )
            .perform(
                click()
            )
            .check(
                matches(isDisplayed())
            )
    }
}