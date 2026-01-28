package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class EventBuilderTest {
    
    @Test
    fun `buildScreenView creates correct event`() {
        val event = EventBuilder.buildScreenView(UIScreen.Main)
        
        assertEquals("screen_view", event.name)
        assertEquals("main", event.screenName)
        assertTrue(event.isManual)
    }
    
    @Test
    fun `buildScreenView creates event with timestamp`() {
        val before = System.currentTimeMillis()
        val event = EventBuilder.buildScreenView(UIScreen.Splash)
        val after = System.currentTimeMillis()
        
        assertTrue(event.timestamp >= before)
        assertTrue(event.timestamp <= after)
    }
    
    @Test
    fun `buildUIInteraction with screen context`() {
        val event = EventBuilder.buildUIInteraction(
            elementId = "btn_buy",
            action = UIAction.Click,
            currentScreenName = "main"
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertEquals("main", event.screenName)
    }
    
    @Test
    fun `buildUIInteraction without screen context`() {
        val event = EventBuilder.buildUIInteraction(
            elementId = "btn_buy",
            action = UIAction.Click,
            currentScreenName = null
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertNull(event.screenName)
    }
    
    @Test
    fun `buildUIInteraction with custom action`() {
        val customAction = UIAction("swipe")
        val event = EventBuilder.buildUIInteraction(
            elementId = "card_item",
            action = customAction,
            currentScreenName = "product_list"
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("card_item", event.elementId)
        assertEquals("swipe", event.action)
        assertEquals("product_list", event.screenName)
    }
}