// src/main/kotlin/com/app/traceless/analytic/Analytics.kt
package com.app.traceless.analytic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Public API for TRACELESS SDK
 * 
 * Usage:
 *   Analytics.enterScreen(Screen.Home)
 *   Analytics.trackUI("btn_buy", UIAction.Click)
 *   Analytics.events.collect { event -> ... }
 */
object Analytics {
    
    private val _state = _State()
    private val _eventChannel = MutableSharedFlow<TracelessEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val scope = CoroutineScope(Dispatchers.Default)
    private var _debugMode = false
    
    /**
     * Public Flow of events for Firebase adapter to collect
     */
    val events: SharedFlow<TracelessEvent> = _eventChannel.asSharedFlow()
    
    /**
     * Enter a new screen. Emits screen_view event.
     * 
     * @param screen The screen to enter (must be in Screen registry)
     * @throws IllegalArgumentException if screen is not in registry
     */
    fun enterScreen(screen: UIScreen) {
        require(screen.name.isNotBlank()) { "Screen name cannot be blank" }
        
        if (_debugMode) {
            Timber.d("[Traceless] ENTER_SCREEN: ${screen.name}")
        }
        
        val event = EventBuilder.buildScreenView(screen)
        _state.updateScreen(screen.name)
        emit(event)
    }
    
    /**
     * Track UI interaction. Emits ui_interaction event with current screen context.
     * 
     * @param elementId The element identifier (e.g., "btn_buy")
     * @param action The UI action (click, submit, scroll, or custom)
     */
    fun trackUI(elementId: String, action: UIAction) {
        require(elementId.isNotBlank()) { "Element ID cannot be blank" }
        
        if (_debugMode) {
            Timber.d("[Traceless] TRACK_UI: $elementId ${action.value} (screen: ${_state.currentScreenName ?: "none"})")
        }
        
        val event = EventBuilder.buildUIInteraction(
            elementId = elementId,
            action = action,
            currentScreenName = _state.currentScreenName
        )
        emit(event)
    }
    
    /**
     * Reset internal state (e.g., on new session)
     * Call this when Firebase session changes
     */
    fun resetState() {
        if (_debugMode) {
            Timber.d("[Traceless] RESET_STATE: clearing current screen and session data")
        }
        _state.reset()
    }
    
    private fun emit(event: TracelessEvent) {
        if (_debugMode) {
            Timber.d("[Traceless] EMIT_EVENT: ${event.name} (screen: ${event.screenName ?: "none"})")
        }
        scope.launch {
            _eventChannel.emit(event)
        }
    }
    
    /**
     * Initialize SDK state (call from Application.onCreate())
     */
    fun initialize() {
        if (_debugMode) {
            Timber.d("[Traceless] INIT: initializing Traceless Analytics SDK")
        }
        // Reset state on init
        _state.reset()
    }
}