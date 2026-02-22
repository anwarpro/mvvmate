package com.helloanwar.mvvmate.core

import kotlin.reflect.KClass

/**
 * Utility for computing human-readable diffs between two state snapshots.
 *
 * Works with any `data class` by reflecting on its `toString()` representation
 * to extract property values and compare them field-by-field.
 *
 * ```kotlin
 * val diff = StateDiffUtil.diff(oldState, newState)
 * // Returns: ["isLoading: false → true", "error: null → \"Network error\""]
 * ```
 */
object StateDiffUtil {

    /**
     * A single changed field with its old and new values.
     */
    data class FieldChange(
        val field: String,
        val oldValue: String,
        val newValue: String
    ) {
        override fun toString(): String = "$field: $oldValue → $newValue"
    }

    /**
     * Compute the diff between two state objects.
     *
     * @param oldState The previous state.
     * @param newState The current state.
     * @return A list of [FieldChange] entries for each changed property.
     *         Returns empty list if states are equal.
     */
    fun <S : UiState> diff(oldState: S, newState: S): List<FieldChange> {
        if (oldState == newState) return emptyList()

        val oldFields = parseFields(oldState.toString(), oldState::class)
        val newFields = parseFields(newState.toString(), newState::class)

        if (oldFields.isEmpty() || newFields.isEmpty()) {
            // Fallback: can't parse, just return a single "state changed" entry
            return listOf(
                FieldChange(
                    field = "state",
                    oldValue = oldState.toString(),
                    newValue = newState.toString()
                )
            )
        }

        return oldFields.mapNotNull { (key, oldValue) ->
            val newValue = newFields[key]
            if (newValue != null && oldValue != newValue) {
                FieldChange(field = key, oldValue = oldValue, newValue = newValue)
            } else null
        }
    }

    /**
     * Format a diff as a compact one-line summary.
     *
     * Example: `"isLoading: false → true, error: null → \"Timeout\""`
     */
    fun <S : UiState> diffSummary(oldState: S, newState: S): String {
        val changes = diff(oldState, newState)
        return when {
            changes.isEmpty() -> "(no changes)"
            else -> changes.joinToString(", ") { it.toString() }
        }
    }

    /**
     * Parse a data class `toString()` output into a map of field name → value string.
     *
     * `UserState(isLoading=true, users=[], error=null)` →
     * `{isLoading: "true", users: "[]", error: "null"}`
     */
    internal fun parseFields(toString: String, klass: KClass<*>): Map<String, String> {
        val className = klass.simpleName ?: return emptyMap()
        val prefix = "$className("
        if (!toString.startsWith(prefix) || !toString.endsWith(")")) {
            return emptyMap()
        }

        val content = toString.removePrefix(prefix).removeSuffix(")")
        if (content.isEmpty()) return emptyMap()

        return splitFields(content)
    }

    /**
     * Split a comma-separated field string while respecting nested brackets and parentheses.
     * Handles: `isLoading=true, items=[Item(a=1), Item(a=2)], error=null`
     */
    private fun splitFields(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var depth = 0 // Tracks nested (), [], {}
        var currentStart = 0

        for (i in content.indices) {
            when (content[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) {
                    parseField(content.substring(currentStart, i).trim())?.let { (k, v) ->
                        result[k] = v
                    }
                    currentStart = i + 1
                }
            }
        }

        // Last field
        parseField(content.substring(currentStart).trim())?.let { (k, v) ->
            result[k] = v
        }

        return result
    }

    private fun parseField(field: String): Pair<String, String>? {
        val eqIndex = field.indexOf('=')
        if (eqIndex <= 0) return null
        val key = field.substring(0, eqIndex).trim()
        val value = field.substring(eqIndex + 1).trim()
        return key to value
    }
}
