import com.helloanwar.mvvmate.core.*
import com.helloanwar.mvvmate.core.ai.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AiActionBridgeTest {

    data class MockState(val count: Int) : UiState
    sealed class MockAction : UiAction {
        object Increment : MockAction()
        object DeleteAccount : MockAction() // Dangerous action
    }

    // A mock ViewModel for testing the bridge
    class MockViewModel : BaseViewModel<MockState, MockAction>(MockState(0)) {
        override suspend fun onAction(action: MockAction) {
            when (action) {
                MockAction.Increment -> updateState { copy(count = count + 1) }
                MockAction.DeleteAccount -> updateState { copy(count = -1) }
            }
        }
    }

    class MockParser : AiActionParser<MockAction> {
        override fun parse(rawCommand: String): MockAction {
            return when (rawCommand) {
                "{\"type\":\"Increment\"}" -> MockAction.Increment
                "{\"type\":\"DeleteAccount\"}" -> MockAction.DeleteAccount
                else -> throw IllegalArgumentException("Unknown command: $rawCommand")
            }
        }
    }

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBridgeExecutesAllowedAction() = runTest {
        val viewModel = MockViewModel()
        
        // Custom policy only allowing Increment
        val safePolicy = object : AiActionPolicy<MockState, MockAction> {
            override fun isActionAllowed(action: MockAction, currentState: MockState): Boolean {
                return action is MockAction.Increment
            }
        }

        val bridge = AiActionBridge(
            viewModel = viewModel,
            policy = safePolicy,
            parser = MockParser()
        )

        // Attempt to dispatch a permitted command
        val result = bridge.dispatch("{\"type\":\"Increment\"}")

        assertTrue(result.isSuccess)
        advanceUntilIdle()
        
        // Ensure state changed correctly via handleAction
        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun testBridgeBlocksDeniedAction() = runTest {
        val viewModel = MockViewModel()
        
        // Custom policy only allowing Increment
        val safePolicy = object : AiActionPolicy<MockState, MockAction> {
            override fun isActionAllowed(action: MockAction, currentState: MockState): Boolean {
                return action is MockAction.Increment
            }
        }

        val bridge = AiActionBridge(
            viewModel = viewModel,
            policy = safePolicy,
            parser = MockParser()
        )

        // Attempt to dispatch a forbidden command
        val result = bridge.dispatch("{\"type\":\"DeleteAccount\"}")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AiSecurityException)
        // Ensure state was NOT mutated
        assertEquals(0, viewModel.state.value.count) // Assuming initial was 0
    }

    @Test
    fun testGetCurrentStateIsRedacted() {
        val viewModel = MockViewModel()
        val bridge = AiActionBridge(
            viewModel = viewModel,
            policy = AllowAllPolicy(), 
            parser = MockParser(),
            // Custom redactor just to prove hook works
            redactor = object : PrivacyRedactor {
                override fun redact(input: String) = "RedactedState"
            }
        )

        assertEquals("RedactedState", bridge.getCurrentState())
    }
}
