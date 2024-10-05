import com.helloanwar.mvvmate.core.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Test class for BaseViewModel
class BaseViewModelTest {

    private lateinit var viewModel: BaseViewModel<TestUiState, TestUiAction>
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = object : BaseViewModel<TestUiState, TestUiAction>(TestUiState()) {
            override suspend fun onAction(action: TestUiAction) {
                when (action) {
                    is TestUiAction.SampleAction -> updateState { copy(value = "Updated") }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        // Ensure the initial state is correct
        assertEquals("", viewModel.state.first().value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testStateUpdateOnAction() = runTest {
        // Trigger action and verify state update
        viewModel.handleAction(TestUiAction.SampleAction)
        advanceUntilIdle() // To ensure all coroutines finish
        assertEquals("Updated", viewModel.state.first().value)
    }
}