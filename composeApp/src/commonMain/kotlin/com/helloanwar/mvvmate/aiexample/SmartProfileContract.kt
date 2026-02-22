package com.helloanwar.mvvmate.aiexample

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState
import kotlinx.serialization.Serializable

data class SmartProfileState(
    val name: String = "",
    val city: String = "",
    val email: String = "",
    val isAiThinking: Boolean = false,
    val aiError: String? = null
) : UiState

@Serializable
sealed class SmartProfileAction : UiAction {
    @Serializable
    data class UpdateField(val field: String, val value: String) : SmartProfileAction()
    
    // Commands that don't go to AI bridge directly:
    data class ProcessNaturalLanguage(val prompt: String, val apiKey: String) : SmartProfileAction()
    object DismissError : SmartProfileAction()
}

sealed class SmartProfileEffect : UiEffect {
    data class ShowToast(val message: String) : SmartProfileEffect()
}
