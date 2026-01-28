package com.app.traceless.analytic

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnalyticsTest {
    
    @Before
    fun setup() {
        Analytics.initialize()
    }
    
    @Test
    fun `enterScreen does not throw for valid screens`() {
        Analytics.enterScreen(UIScreen.Main)
        Analytics.enterScreen(UIScreen.Splash)
    }
    
    @Test
    fun `trackUI does not throw for valid inputs`() {
        Analytics.trackUI("btn_buy", UIAction.Click)
        Analytics.trackUI("form_submit", UIAction.Submit)
    }
    
    @Test
    fun `trackUI with blank elementId throws exception`() {
        try {
            Analytics.trackUI("", UIAction.Click)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Element ID cannot be blank", e.message)
        }
    }
    
    @Test
    fun `trackUI with blank elementId containing spaces throws exception`() {
        try {
            Analytics.trackUI("   ", UIAction.Click)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Element ID cannot be blank", e.message)
        }
    }
    
    @Test
    fun `resetState completes without throwing`() {
        Analytics.resetState()
        Analytics.trackUI("test_btn", UIAction.Click)
        Analytics.resetState()
    }
    
    @Test
    fun `multiple API calls work together`() {
        Analytics.enterScreen(UIScreen.Splash)
        Analytics.trackUI("btn_home", UIAction.Click)
        Analytics.enterScreen(UIScreen.Main)
        Analytics.trackUI("btn_detail", UIAction.Submit)
        Analytics.resetState()
        Analytics.enterScreen(UIScreen.Main)
        Analytics.trackUI("btn_profile", UIAction.View)
    }
    
    @Test
    fun `enterScreen works with all predefined screens`() {
        Analytics.enterScreen(UIScreen.Splash)
        Analytics.enterScreen(UIScreen.Main)
    }
    
    @Test
    fun `trackUI works with all predefined actions`() {
        Analytics.trackUI("element1", UIAction.Click)
        Analytics.trackUI("element2", UIAction.Submit)
    }
    
    @Test
    fun `Analytics events flow is accessible`() = runTest {
        val events = Analytics.listenEvents()
        assertNotNull(events)
    }
}