// src/main/kotlin/com/app/traceless/analytic/ParamKeyValidator.kt
package com.app.traceless.analytic

import timber.log.Timber

/**
 * Validator for custom parameter keys.
 *
 * Enforces snake_case convention with automatic camelCase conversion:
 * - snake_case keys: used as-is
 * - camelCase keys: auto-converted to snake_case
 * - Other formats: throws IllegalArgumentException
 */
internal object ParamKeyValidator {

    /**
     * Validates and normalizes parameter keys to snake_case.
     *
     * @param params Map of custom parameters (can be null)
     * @return Normalized map with all keys in snake_case, or null if input was null
     * @throws IllegalArgumentException if a key is neither snake_case nor camelCase
     */
    fun normalizeParams(params: Map<String, Any>?): Map<String, Any>? {
        if (params == null) return null

        val converted = mutableMapOf<String, Any>()
        val warnings = mutableListOf<String>()

        for ((key, value) in params) {
            when {
                isSnakeCase(key) -> {
                    converted[key] = value
                }
                isCamelCase(key) -> {
                    val snakeKey = camelToSnakeCase(key)
                    converted[snakeKey] = value
                    warnings.add("  • '$key' → '$snakeKey'")
                }
                else -> {
                    throw IllegalArgumentException(
                        "Invalid parameter key '$key'. Must be snake_case or camelCase. " +
                        "Examples: 'user_id', 'itemPrice', 'total_amount'"
                    )
                }
            }
        }

        if (warnings.isNotEmpty()) {
            Timber.tag("Traceless").w(
                "[Traceless] Auto-converted camelCase keys to snake_case:\n${warnings.joinToString("\n")}"
            )
        }

        return converted
    }

    /**
     * Checks if a string follows snake_case convention.
     * Rules: starts with lowercase letter, contains only lowercase letters, numbers, and underscores.
     */
    private fun isSnakeCase(key: String): Boolean {
        if (key.isEmpty()) return false
        return key.matches(Regex("^[a-z][a-z0-9_]*$"))
    }

    /**
     * Checks if a string follows camelCase or PascalCase convention.
     * Rules: starts with letter, contains only letters and numbers, has at least one uppercase letter.
     */
    private fun isCamelCase(key: String): Boolean {
        if (key.isEmpty()) return false
        return key.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*$")) && key.any { it.isUpperCase() }
    }

    /**
     * Converts camelCase or PascalCase to snake_case.
     * Examples:
     * - "userId" → "user_id"
     * - "totalAmount" → "total_amount"
     * - "getItemPrice" → "get_item_price"
     */
    private fun camelToSnakeCase(key: String): String {
        return key.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2].lowercase()}" }
    }
}
