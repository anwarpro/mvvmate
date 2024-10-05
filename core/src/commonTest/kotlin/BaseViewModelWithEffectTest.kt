import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
        // Trigger action and verify side effect emission
        viewModel.handleAction(TestUiAction.SampleAction)
        assertEquals("Test Effect", viewModel.sideEffects.first())
    }
}