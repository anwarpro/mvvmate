package com.helloanwar.mvvmate.generator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

class AiTestGenerator {

    /**
     * Executes a Koog prompt to generate a Kotlin Unit Test using the provided
     * [crashTraceJson] (extracted from MvvMateAiLogger) and [targetClassName].
     *
     * @param crashTraceJson The raw JSON output from the MvvMateAiLogger.
     * @param targetClassName The name of the ViewModel to test (e.g., "SmartProfileViewModel").
     * @param apiKey The Gemini API Key.
     * @return Raw Kotlin source code containing the generated test class.
     */
    suspend fun generateTest(
        crashTraceJson: String,
        targetClassName: String,
        apiKey: String
    ): String {
        val systemPrompt = """
            You are an expert Android/Kotlin Multiplatform Developer specializing in MVI architectures.
            Your task is to parse the MVVMate chronological crash trace JSON and write a functional JUnit test for the corresponding ViewModel.

            1. The User will provide you with a JSON array that represents the chronological sequence of `UiAction`s and `UiState` change diffs.
            2. You must output EXACTLY valid Kotlin code starting with imports and the class definition. Do NOT wrap the output in markdown blocks (e.g., ```kotlin). Just return raw source code.
            3. Use the `kotlin.test.*` package (e.g., `@Test`, `assertEquals`).
            4. Assume the ViewModel is called `$targetClassName`.
            5. The test should initialize the ViewModel, dispatch the actions exactly as they appear in the trace, and assert the final state based on the trace's state changes.
            6. Add a placeholder `// TODO: Resolve package imports` at the top of the file so the user knows they need to fix package names.
            7. Use `runTest` from `kotlinx.coroutines.test` for coroutines.
        """.trimIndent()

        // Using simpleGoogleAIExecutor backed by Gemini as per recent Koog updates in the project
        val agent = AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            systemPrompt = systemPrompt,
            llmModel = GoogleModels.Gemini2_5FlashLite
        )

        val prompt = "Generate a Kotlin test class for $targetClassName based on this crash trace:\n$crashTraceJson"
        
        return try {
            val result = agent.run(prompt)
            // Cleanup any accidental markdown if the model hallucinates formatting
            result.removePrefix("```kotlin\n").removePrefix("```\n").removeSuffix("```").trim()
        } catch (e: Exception) {
            "// Failed to generate test: ${e.message}\n// Please ensure the API key and trace are valid."
        }
    }
}
