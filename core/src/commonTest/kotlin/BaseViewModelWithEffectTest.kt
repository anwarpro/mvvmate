import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Test class for BaseViewModelWithEffect
@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelWithEffectTest {

    private lateinit var viewModel: BaseViewModelWithEffect<TestUiState, TestUiAction, String>
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = object : BaseViewModelWithEffect<TestUiState, TestUiAction, String>(TestUiState()) {
            override suspend fun onAction(action: TestUiAction) {
                emitSideEffect("Test Effect")
            }
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSideEffectEmission() = runTest {
        val effects = mutableListOf<String>()

        // Start collecting effects before dispatching action
        val collectJob = launch {
            viewModel.sideEffects.collect { effects.add(it) }
        }

        // Trigger action
        viewModel.handleAction(TestUiAction.SampleAction)
        advanceUntilIdle()

        // Verify effect was received
        assertEquals(1, effects.size)
        assertEquals("Test Effect", effects.first())
        collectJob.cancel()
    }
}