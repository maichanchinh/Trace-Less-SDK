package com.app.traceless.analytic

/**
 * Data class representing an analytics event emitted by the SDK.
 *
 * @property name The event name (e.g., "screen_view", "ui_interaction")
 * @property params Map of event parameters
 * @property timestamp Unix timestamp when event was created
 */
data class TracelessEvent(
    val name: String,
    val params: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Get screen_name from params, null if not present */
    val screenName: String? get() = params["screen_name"] as? String
    
    /** Get element_id from params, null if not present */
    val elementId: String? get() = params["element_id"] as? String
    
    /** Get action from params, null if not present */
    val action: String? get() = params["action"] as? String
    
    /** Get is_manual from params, defaults to false */
    val isManual: Boolean get() = params["is_manual"] as? Boolean ?: false
}