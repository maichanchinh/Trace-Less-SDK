package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class TracelessEventTest {
    
    @Test
    fun `create screen_view event with correct params`() {
        val event = TracelessEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "home",
                "is_manual" to true
            )
        )
        
        assertEquals("screen_view", event.name)
        assertEquals("home", event.screenName)
        assertTrue(event.isManual)
        assertNull(event.elementId)
        assertNull(event.action)
    }
    
    @Test
    fun `create ui_interaction event with all params`() {
        val event = TracelessEvent(
            name = "ui_interaction",
            params = mapOf(
                "element_id" to "btn_buy",
                "action" to "click",
                "screen_name" to "home"
            )
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertEquals("home", event.screenName)
    }
    
    @Test
    fun `event timestamp is auto-generated`() {
        val before = System.currentTimeMillis()
        val event = TracelessEvent(
            name = "test",
            params = emptyMap()
        )
        val after = System.currentTimeMillis()
        
        assertTrue(event.timestamp >= before)
        assertTrue(event.timestamp <= after)
    }
    
    @Test
    fun `extension properties return null for missing params`() {
        val event = TracelessEvent(
            name = "test",
            params = emptyMap()
        )
        
        assertNull(event.screenName)
        assertNull(event.elementId)
        assertNull(event.action)
        assertFalse(event.isManual)
    }
}