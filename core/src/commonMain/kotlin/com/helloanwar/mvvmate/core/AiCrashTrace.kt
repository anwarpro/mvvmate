package com.helloanwar.mvvmate.core

/**
 * Represents a single chronological event recorded by the [MvvMateAiLogger].
 */
sealed interface AiLogEvent {
    val sequenceId: Long

    data class Action(
        override val sequenceId: Long,
        val viewModelName: String,
        val actionClass: String,
        val actionDetails: String
    ) : AiLogEvent

    data class StateChange(
        override val sequenceId: Long,
        val viewModelName: String,
        val diffSummary: String,
        val fullStateDump: String
    ) : AiLogEvent

    data class Effect(
        override val sequenceId: Long,
        val viewModelName: String,
        val effectClass: String,
        val effectDetails: String
    ) : AiLogEvent

    data class Error(
        override val sequenceId: Long,
        val viewModelName: String,
        val exceptionClass: String,
        val errorMessage: String?,
        val context: String
    ) : AiLogEvent

    data class Network(
        override val sequenceId: Long,
        val tag: String,
        val phase: String,
        val details: String
    ) : AiLogEvent
}

/**
 * A chronological trail of recent events leading up to a crash or generated upon request.
 * Useful for feeding into an LLM for debugging purposes.
 */
data class CrashTrace(
    val events: List<AiLogEvent>
) {
    /**
     * Formats the trace into a human/LLM-readable chronological digest.
     */
    fun formatReadableDigest(): String {
        if (events.isEmpty()) return "No events recorded."
        
        val sb = StringBuilder()
        sb.appendLine("--- AI Crash Trace Start ---")
        
        events.forEachIndexed { index, event ->
            sb.appendLine("[$index] Event #${event.sequenceId}")
            when (event) {
                is AiLogEvent.Action -> {
                    sb.appendLine("  -> ACTION: [${event.viewModelName}] ${event.actionClass}")
                    sb.appendLine("     Details: ${event.actionDetails}")
                }
                is AiLogEvent.StateChange -> {
                    sb.appendLine("  -> STATE CHANGE: [${event.viewModelName}]")
                    sb.appendLine("     Diff: ${event.diffSummary}")
                    sb.appendLine("     State: ${event.fullStateDump}")
                }
                is AiLogEvent.Effect -> {
                    sb.appendLine("  -> EFFECT: [${event.viewModelName}] ${event.effectClass}")
                    sb.appendLine("     Details: ${event.effectDetails}")
                }
                is AiLogEvent.Error -> {
                    sb.appendLine("  -> ERROR: [${event.viewModelName}] ${event.exceptionClass}")
                    sb.appendLine("     Context: ${event.context}")
                    sb.appendLine("     Message: ${event.errorMessage}")
                }
                is AiLogEvent.Network -> {
                    sb.appendLine("  -> NETWORK: [${event.tag}] ${event.phase}")
                    if (event.details.isNotBlank()) sb.appendLine("     Details: ${event.details}")
                }
            }
            sb.appendLine()
        }
        sb.appendLine("--- AI Crash Trace End ---")
        return sb.toString()
    }
}
