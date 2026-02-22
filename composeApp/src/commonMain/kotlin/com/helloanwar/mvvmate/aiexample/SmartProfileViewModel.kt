package com.helloanwar.mvvmate.aiexample

import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import com.helloanwar.mvvmate.core.ai.AiActionBridge
import com.helloanwar.mvvmate.core.ai.AiActionParser
import com.helloanwar.mvvmate.core.ai.AiActionPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SmartProfileViewModel :
    BaseViewModelWithEffect<SmartProfileState, SmartProfileAction, SmartProfileEffect>(
        SmartProfileState()
    ) {
    private val koogHandler = GeminiKoogHandler()

    // Strict parser mapping LLM JSON to Kotlin Data Class
    private val actionParser = object : AiActionParser<SmartProfileAction> {
        override fun parse(rawCommand: String): SmartProfileAction {
            // We expect an array or single object from the handler, for simplicity we parse individual items fed to it
            // So this parser handles a single {"field":"...", "value":"..."} object.
            val element = Json.parseToJsonElement(rawCommand).jsonObject
            val field = element["field"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing field")
            val value = element["value"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing value")

            return SmartProfileAction.UpdateField(field, value)
        }
    }

    // A security policy demonstrating that the AI is only allowed to use UpdateField
    private val aiPolicy = object : AiActionPolicy<SmartProfileState, SmartProfileAction> {
        override fun isActionAllowed(
            action: SmartProfileAction,
            currentState: SmartProfileState
        ): Boolean {
            return action is SmartProfileAction.UpdateField
        }
    }

    private val bridge = AiActionBridge(
        viewModel = this,
        policy = aiPolicy,
        parser = actionParser
    )

    override suspend fun onAction(action: SmartProfileAction) {
        when (action) {
            is SmartProfileAction.UpdateField -> {
                updateState {
                    when (action.field.lowercase()) {
                        "name" -> copy(name = action.value)
                        "city" -> copy(city = action.value)
                        "email" -> copy(email = action.value)
                        else -> this
                    }
                }
            }

            is SmartProfileAction.DismissError -> {
                updateState { copy(aiError = null) }
            }

            is SmartProfileAction.ProcessNaturalLanguage -> {
                if (action.apiKey.isBlank()) {
                    updateState { copy(aiError = "API Key is required!") }
                    return
                }

                updateState { copy(isAiThinking = true, aiError = null) }

                try {
                    // 1. Send natural language to Gemini via Koog
                    val jsonResponseString =
                        koogHandler.parseNaturalLanguage(action.prompt, action.apiKey)

                    // The LLM returns a JSON Array. Parse array and dispatch each item individually through bridge
                    val jsonArray = Json.parseToJsonElement(jsonResponseString).jsonArray

                    if (jsonArray.isEmpty()) {
                        emitSideEffect(SmartProfileEffect.ShowToast("No fields detected in prompt."))
                    } else {
                        var successCount = 0
                        for (item in jsonArray) {
                            val result = bridge.dispatch(item.toString())
                            if (result.isSuccess) successCount++
                        }
                        emitSideEffect(SmartProfileEffect.ShowToast("Auto-filled $successCount field(s)"))
                    }
                } catch (e: Exception) {
                    updateState { copy(aiError = e.message ?: "Failed to contact Gemini") }
                } finally {
                    updateState { copy(isAiThinking = false) }
                }
            }
        }
    }
}
