package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class ScreenTest {
    
    @Test
    fun `Home screen has correct name`() {
        assertEquals("main", UIScreen.Main.name)
    }
    
    @Test
    fun `Detail screen has correct name`() {
        assertEquals("splash", UIScreen.Splash.name)
    }
    
}