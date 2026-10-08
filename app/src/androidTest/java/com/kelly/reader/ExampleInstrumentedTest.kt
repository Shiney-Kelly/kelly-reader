package com.kelly.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example instrumented test. It runs on a device and checks the app's package name.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun packageName_appContext_matchesApplicationId() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.kelly.reader", appContext.packageName)
    }
}
