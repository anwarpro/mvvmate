import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState

data class TestUiState(val value: String = "") : UiState
sealed class TestUiAction : UiAction {
    data object SampleAction : TestUiAction()
}