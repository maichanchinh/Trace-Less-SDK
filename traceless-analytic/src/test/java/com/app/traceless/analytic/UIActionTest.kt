package com.app.traceless.analytic

import com.app.traceless.analytic.UIAction.*
import org.junit.Assert.*
import org.junit.Test

class UIActionTest {
    
    @Test
    fun `Click action has correct value`() {
        assertEquals("click", Click.value)
    }
    
    @Test
    fun `Submit action has correct value`() {
        assertEquals("submit", Submit.value)
    }

    @Test
    fun `All predefined actions have expected values`() {
        val expected = mapOf(
            Click to "click",
            Select to "select",
            Input to "input",
            Swipe to "swipe",
            Submit to "submit",
            Toggle to "toggle",
            Navigate to "navigate",
            View to "view",
            Refresh to "refresh",
            Dismiss to "dismiss",
        )

        expected.forEach { (action, value) ->
            assertEquals("Action ${action::class.simpleName} should have value '$value'", value, action.value)
        }
    }

    @Test
    fun `Action values are unique`() {
        val values = listOf(
            Click.value,
            Select.value,
            Input.value,
            Swipe.value,
            Submit.value,
            Toggle.value,
            Navigate.value,
            View.value,
            Refresh.value,
            Dismiss.value,
        )

        val unique = values.toSet()
        assertEquals("All UIAction values should be unique", values.size, unique.size)
    }

    @Test
    fun `Same object references are equal and different actions are not equal`() {
        // same reference
        assertTrue(Click === Click)
        assertEquals(Click, Click)

        // different references
        assertNotEquals(Click.value, Submit.value)
        assertFalse(Click.value === Submit.value)
    }
}