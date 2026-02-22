package com.helloanwar.mvvmate.aiexample

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

class GeminiKoogHandler {

    /**
     * Takes the user's natural language, uses Koog to hit Gemini 1.5 Flash,
     * and forces the LLM to output a JSON array of `UpdateField` actions.
     */
    suspend fun parseNaturalLanguage(promptStr: String, apiKey: String): String {
        // According to Koog documentation
        val agent = AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            systemPrompt = """
                You are a smart form assistant. Extract 'name', 'city', and 'email' from the user's prompt.
                
                Respond ONLY with a valid JSON array of objects representing the fields to update. Do not include markdown formatting or explanation.
                Format: [{"type":"com.helloanwar.mvvmate.aiexample.SmartProfileAction.UpdateField", "field":"name", "value":"John Doe"}, ...]
                
                If a field is not mentioned, do not include it.
            """.trimIndent(),
            llmModel = GoogleModels.Gemini2_5FlashLite
        )

        return agent.run(promptStr).trim()
    }
}
