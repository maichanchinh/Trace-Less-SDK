// src/main/kotlin/com/app/traceless/analytic/EventBuilder.kt
package com.app.traceless.analytic

internal object EventBuilder {
    
    fun buildScreenView(screen: UIScreen): TracelessEvent {
        return TracelessEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to screen.name,
                "is_manual" to true
            )
        )
    }
    
    fun buildUIInteraction(
        elementId: String,
        action: UIAction,
        currentScreenName: String?
    ): TracelessEvent {
        val params = mutableMapOf<String, Any>(
            "element_id" to elementId,
            "action" to action.value
        )
        currentScreenName?.let {
            params["screen_name"] = it
        }
        return TracelessEvent(
            name = "ui_interaction",
            params = params
        )
    }
}