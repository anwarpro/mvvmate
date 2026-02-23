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
