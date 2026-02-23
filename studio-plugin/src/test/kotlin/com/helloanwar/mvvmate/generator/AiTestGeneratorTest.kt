package com.helloanwar.mvvmate.generator

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTestGeneratorTest {

    @Test
    fun `generateTest returns placeholder on invalid token`() = runBlocking {
        val generator = AiTestGenerator()
        val result = generator.generateTest(
            crashTraceJson = "[{\"event\":\"FakeData\"}]",
            targetClassName = "TestViewModel",
            apiKey = "invalid_key_123"
        )
        
        // With an invalid key, simpleOpenAIExecutor typically throws
        // which the method catches and returns the error comment block.
        assertTrue(result.startsWith("// Failed to generate test:"))
    }
    
    // We mock/fake other calls as needed but Koog is making external calls.
    // An end-to-end integration test is often manual for plugins or uses Sandbox.
}
