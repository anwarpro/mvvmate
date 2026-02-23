package com.helloanwar.mvvmate.testing

import com.helloanwar.mvvmate.core.BaseViewModel
import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

// -------------------------------------------------------------------------------------------------
// Test Dummies
// -------------------------------------------------------------------------------------------------

data class TestState(val count: Int = 0, val isLoading: Boolean = false) : UiState

sealed interface TestAction : UiAction {
    data object Increment : TestAction
    data object ComplexStart : TestAction
    data class Submit(val value: String) : TestAction
}

sealed interface TestEffect : UiEffect {
    data object ShowToast : TestEffect
    data class Navigate(val destination: String) : TestEffect
}

class SimpleTestViewModel : BaseViewModel<TestState, TestAction>(TestState()) {
    override suspend fun onAction(action: TestAction) {
        when (action) {
            TestAction.Increment -> updateState { copy(count = count + 1) }
            TestAction.ComplexStart -> {
                updateState { copy(isLoading = true) }
                delay(10)
                updateState { copy(count = count + 10, isLoading = false) }
            }
            is TestAction.Submit -> {} // No-op
        }
    }
}

class EffectTestViewModel : BaseViewModelWithEffect<TestState, TestAction, TestEffect>(TestState()) {
    override suspend fun onAction(action: TestAction) {
        when (action) {
            TestAction.Increment -> {
                updateState { copy(count = count + 1) }
                emitSideEffect(TestEffect.ShowToast)
            }
            TestAction.ComplexStart -> {
                updateState { copy(isLoading = true) }
                emitSideEffect(TestEffect.Navigate("loading"))
                delay(10)
                updateState { copy(count = count + 10, isLoading = false) }
                emitSideEffect(TestEffect.Navigate("done"))
            }
            is TestAction.Submit -> {
                emitSideEffect(TestEffect.ShowToast)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Unit Tests
// -------------------------------------------------------------------------------------------------

class ViewModelTestDslTest {

    @Test
    fun testInitialStateIsEmitted() = runTest {
        val vm = SimpleTestViewModel()
        vm.test {
            val initialState = awaitState()
            assertEquals(0, initialState.count)
        }
    }

    @Test
    fun testSimpleActionUpdatesState() = runTest {
        val vm = SimpleTestViewModel()
        vm.test {
            expectStateEquals(TestState(0, false))
            
            dispatchAction(TestAction.Increment)
            
            expectStateEquals(TestState(1, false))
        }
    }

    @Test
    fun testComplexActionYieldsMultipleStates() = runTest {
        val vm = SimpleTestViewModel()
        vm.test {
            expectState { !it.isLoading }
            
            dispatchAction(TestAction.ComplexStart)
            
            // First it sets loading
            expectState { it.isLoading }
            
            // Then it finishes
            val finalState = expectState { !it.isLoading }
            assertEquals(10, finalState.count)
        }
    }

    @Test
    fun testAssertionFailureThrows() = runTest {
        val vm = SimpleTestViewModel()
        
        assertFailsWith<AssertionError> {
            vm.test {
                expectStateEquals(TestState(999, true)) // Wrong initial state
            }
        }
    }
}

class ViewModelEffectTestDslTest {

    @Test
    fun testInitialStateIsEmittedForEffectViewModel() = runTest {
        val vm = EffectTestViewModel()
        vm.testEffects {
            val initialState = expectState { it.count == 0 }
            assertEquals(0, initialState.count)
        }
    }

    @Test
    fun testStateAndEffectAreInterleavedInCorrectOrder() = runTest {
        val vm = EffectTestViewModel()
        
        vm.testEffects {
            // Initial state
            expectStateEquals(TestState(0, false))
            
            dispatchAction(TestAction.Increment)
            
            // First we expect the state update
            expectStateEquals(TestState(1, false))
            
            // Immediately followed by the effect
            expectEffectEquals(TestEffect.ShowToast)
        }
    }

    @Test
    fun testComplexActionYieldsStatesAndEffects() = runTest {
        val vm = EffectTestViewModel()
        
        vm.testEffects {
            skipEvents(1) // Skip initial state
            
            dispatchAction(TestAction.ComplexStart)
            
            expectStateEquals(TestState(0, true))
            expectEffectEquals(TestEffect.Navigate("loading"))
            
            expectStateEquals(TestState(10, false))
            expectEffectEquals(TestEffect.Navigate("done"))
        }
    }

    @Test
    fun testExpectEffectClassWorks() = runTest {
        val vm = EffectTestViewModel()
        
        vm.testEffects {
            skipEvents(1) // skip initial state
            
            dispatchAction(TestAction.Submit("hello"))
            
            // Doesn't yield a state change, just an effect
            val effect = expectEffectClass<TestEffect.ShowToast>()
            assertEquals(TestEffect.ShowToast, effect)
        }
    }

    @Test
    fun testWrongExpectationThrowsEffectInsteadOfState() = runTest {
        val vm = EffectTestViewModel()
        
        assertFailsWith<AssertionError> {
            vm.testEffects {
                skipEvents(1)
                dispatchAction(TestAction.Submit("hello"))
                
                // This will fail because the next emission is ShowToast (Effect), not a State
                expectState { true }
            }
        }
    }
    
    @Test
    fun testWrongExpectationThrowsStateInsteadOfEffect() = runTest {
        val vm = EffectTestViewModel()
        
        assertFailsWith<AssertionError> {
            vm.testEffects {
                // Initial state emission is State, but we foolishly expect Effect
                expectEffectEquals(TestEffect.ShowToast)
            }
        }
    }
}
