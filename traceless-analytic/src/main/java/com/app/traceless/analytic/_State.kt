package com.app.traceless.analytic

internal class _State {
    var currentScreenName: String? = null
        private set
    
    fun updateScreen(screenName: String) {
        currentScreenName = screenName
    }
    
    fun reset() {
        currentScreenName = null
    }
}