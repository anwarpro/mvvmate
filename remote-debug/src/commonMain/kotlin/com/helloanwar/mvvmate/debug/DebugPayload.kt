package com.helloanwar.mvvmate.debug

import kotlinx.serialization.Serializable

@Serializable
enum class PayloadType {
    ACTION,
    STATE_CHANGE,
    EFFECT,
    ERROR,
    NETWORK
}

@Serializable
data class DebugPayload(
    val type: PayloadType,
    val viewModelName: String,
    val payload: String,
    val additionalData: String? = null,
    val timestamp: Long
)

/**
 * Commands sent FROM the IDE TO the running app.
 */
@Serializable
enum class CommandType {
    /** Inject a UiAction into a ViewModel's handleAction(). */
    INJECT_ACTION,
    /** Override the current UiState of a ViewModel (time-travel). */
    SET_STATE
}

/**
 * A command message from the IDE plugin to the connected app.
 *
 * @param type The kind of command.
 * @param viewModelName Target ViewModel's logTag / class name.
 * @param payload Serialized action class name or state JSON.
 */
@Serializable
data class DebugCommand(
    val type: CommandType,
    val viewModelName: String,
    val payload: String
)
