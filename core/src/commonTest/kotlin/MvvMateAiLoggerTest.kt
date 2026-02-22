import com.helloanwar.mvvmate.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrivacyRedactorTest {
    @Test
    fun testRegexRedactorScrubsEmailsAndPasswords() {
        val redactor = RegexPrivacyRedactor()
        val input = "User test.user@gmail.com logged in with password=\"Secret1234!\" and token: 'abcxyz' using credit card 1234-5678-9012-3456"
        val output = redactor.redact(input)
        
        assertEquals(
            "User [EMAIL_REDACTED] logged in with password=[REDACTED] and token=[REDACTED] using credit card [CREDIT_CARD_REDACTED]", 
            output
        )
    }
}

class MvvMateAiLoggerTest {

    data class MockState(val count: Int) : UiState
    sealed class MockAction : UiAction {
        object Increment : MockAction()
    }

    @Test
    fun testBufferMaintainsMaxSize() {
        // Set max size to 3
        val logger = MvvMateAiLogger(maxHistorySize = 3)
        
        // Add 5 events
        repeat(5) {
            logger.logAction("TestVM", MockAction.Increment)
        }
        
        val snapshot = logger.takeSnapshot()
        assertEquals(3, snapshot.events.size)
        
        // Since sequence starts from 1, and we logged 5 actions, 
        // the remaining events should have sequenceIds 3, 4, and 5.
        // Wait, repeat(5) runs index 0..4.
        // the sequences will be 1, 2, 3, 4, 5. Left over: 3, 4, 5.
        assertEquals(3L, snapshot.events[0].sequenceId)
        assertEquals(4L, snapshot.events[1].sequenceId)
        assertEquals(5L, snapshot.events[2].sequenceId)
    }

    @Test
    fun testTraceFormattingAndRedaction() {
        val logger = MvvMateAiLogger(maxHistorySize = 10)
        
        // Add an action
        logger.logAction("TestVM", MockAction.Increment)
        // Add a state change with sensitive info
        logger.logStateChange(
            "TestVM", 
            MockState(0), 
            MockState(1) // Assuming StateDiffUtil handles this safely or toString has sensitive info. We'll simulate by manually appending to diff logic in real scenarios
        )
        // Force an error log with sensitive info
        logger.logError("TestVM", RuntimeException("Failed auth for user@gmail.com with password=1234"), "LoginFlow")
        
        val traceString = logger.takeRedactedSnapshotString()
        
        assertTrue(traceString.contains("ACTION: [TestVM] Increment"), "Trace should contain Action details")
        assertTrue(traceString.contains("STATE CHANGE: [TestVM]"), "Trace should contain State Change details")
        assertTrue(traceString.contains("ERROR: [TestVM] RuntimeException"), "Trace should contain Error details")
        
        // Verify redaction was applied to the formatted trace automatically
        assertTrue(traceString.contains("[EMAIL_REDACTED]"), "Email should be redacted in trace output")
        assertTrue(traceString.contains("password=[REDACTED]"), "Password should be redacted in trace output")
    }
}
