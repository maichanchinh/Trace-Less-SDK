// src/main/kotlin/com/app/traceless/analytic/EventBuilder.kt
package com.app.traceless.analytic

internal object EventBuilder {

    fun buildScreenView(
        screen: UIScreen,
        customParams: Map<String, Any>? = null
    ): TracelessEvent {
        val params = mutableMapOf<String, Any>(
            "screen_name" to screen.name,
            "is_manual" to true
        )
        // Merge custom params
        customParams?.let { params.putAll(it) }
        return TracelessEvent(
            name = "screen_view",
            params = params
        )
    }

    fun buildUIInteraction(
        elementId: String,
        action: UIAction,
        currentScreenName: String?,
        customParams: Map<String, Any>? = null
    ): TracelessEvent {
        val params = mutableMapOf<String, Any>(
            "element_id" to elementId,
            "action" to action.value
        )
        currentScreenName?.let {
            params["screen_name"] = it
        }
        // Merge custom params
        customParams?.let { params.putAll(it) }
        return TracelessEvent(
            name = "ui_interaction",
            params = params
        )
    }
}