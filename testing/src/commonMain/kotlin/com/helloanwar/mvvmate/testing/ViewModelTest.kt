package com.helloanwar.mvvmate.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.helloanwar.mvvmate.core.BaseViewModel
import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map

/**
 * A testing DSL scope for validating a [BaseViewModel]'s state sequence.
 * This class wraps a Turbine [ReceiveTurbine] internally to provide exact assertions.
 *
 * @param S The type of UI state.
 * @param A The type of user action.
 */
public class ViewModelTestScope<S : UiState, A : UiAction>(
    private val actionHandler: (A) -> Unit,
    public val turbine: ReceiveTurbine<S>
) {
    /**
     * The [ReceiveTurbine] instance used for testing the flow of states.
     * Can be used for advanced Turbine operations that are not covered by the DSL.
     */
    public val rawTurbine: ReceiveTurbine<S> = turbine

    /**
     * Dispatch an action to the ViewModel under test. This will trigger the ViewModel's `onAction` processing.
     */
    public fun dispatchAction(action: A) {
        actionHandler(action)
    }

    /**
     * Awaits the next state emission and asserts it matches the provided block.
     *
     * @param assertBlock A lambda used to assert conditions on the emitted state.
     * @return The state that was asserted upon, useful for further inspection.
     */
    public suspend fun expectState(assertBlock: (S) -> Boolean): S {
        val state = turbine.awaitItem()
        assertTrue(
            actual = assertBlock(state),
            message = "State assertion failed for state: $state"
        )
        return state
    }

    /**
     * Awaits the next state emission and asserts it is strictly equal to [expectedState].
     *
     * @param expectedState The exact expected state.
     * @return The state that was asserted upon.
     */
    public suspend fun expectStateEquals(expectedState: S): S {
        val state = turbine.awaitItem()
        assertEquals(expectedState, state)
        return state
    }

    /**
     * Awaits the next state emission.
     */
    public suspend fun awaitState(): S {
        return turbine.awaitItem()
    }

    /**
     * Consume and ignore the next [count] states.
     */
    public suspend fun skipStates(count: Int) {
        turbine.skipItems(count)
    }

    /**
     * Cancel the flow collection and ignore any remaining events.
     */
    public suspend fun cancelAndIgnoreRemainingStates() {
        turbine.cancelAndIgnoreRemainingEvents()
    }
}

/**
 * Starts a test block for a [BaseViewModel], allowing dispatch of actions and assertions of the resulting
 * sequence of [UiState]s.
 *
 * Example:
 * ```kotlin
 * viewModel.test {
 *     // Initial state is emitted automatically
 *     val initial = awaitState()
 *     
 *     dispatchAction(CounterAction.Increment)
 *     expectState { it.count == 1 }
 * }
 * ```
 *
 * @param timeout An optional custom timeout for the test.
 * @param name An optional name for the test, useful for debug output.
 * @param validate The test block where actions are dispatched and states are asserted.
 */
public suspend fun <S : UiState, A : UiAction> BaseViewModel<S, A>.test(
    timeout: Duration? = null,
    name: String? = null,
    validate: suspend ViewModelTestScope<S, A>.() -> Unit
) {
    val viewModel = this
    this.state.test(timeout, name) {
        val scope = ViewModelTestScope<S, A>(
            actionHandler = { action -> viewModel.handleAction(action) },
            turbine = this
        )
        scope.validate()
        
        // After validation, ensure no unconsumed items unless explicitly ignored
        cancelAndIgnoreRemainingEvents()
    }
}

// -------------------------------------------------------------------------------------------------

/**
 * A sealed class to multiplex State and Effect emissions into a single chronological stream.
 */
public sealed class ViewModelEvent<out S : UiState, out E> {
    public data class State<S : UiState>(val state: S) : ViewModelEvent<S, Nothing>()
    public data class Effect<E>(val effect: E) : ViewModelEvent<Nothing, E>()
}

/**
 * Wraps [Flow] emissions into [ViewModelEvent]s.
 */

public fun <S : UiState, E> Flow<S>.asStateEvents(): Flow<ViewModelEvent<S, E>> {
    return this.map { ViewModelEvent.State(it) }
}

public fun <S : UiState, E> Flow<E>.asEffectEvents(): Flow<ViewModelEvent<S, E>> {
    return this.map { ViewModelEvent.Effect(it) }
}

/**
 * A testing DSL scope for validating a [BaseViewModelWithEffect]'s interleaved state and effect sequence.
 * This class wraps a Turbine [ReceiveTurbine] internally to provide exact assertions.
 *
 * @param S The type of UI state.
 * @param A The type of user action.
 * @param E The type of UI effect.
 */
public class ViewModelEffectTestScope<S : UiState, A : UiAction, E>(
    private val actionHandler: (A) -> Unit,
    public val turbine: ReceiveTurbine<ViewModelEvent<S, E>>
) {
    /**
     * The [ReceiveTurbine] instance used for testing the flow of events.
     * Can be used for advanced Turbine operations that are not covered by the DSL.
     */
    public val rawTurbine: ReceiveTurbine<ViewModelEvent<S, E>> = turbine

    /**
     * Dispatch an action to the ViewModel under test. This will trigger the ViewModel's `onAction` processing.
     */
    public fun dispatchAction(action: A) {
        actionHandler(action)
    }

    /**
     * Awaits the next emission and asserts that it is a State matching the provided block.
     * Throws an assertion error if an Effect is emitted instead.
     *
     * @param assertBlock A lambda used to assert conditions on the emitted state.
     * @return The state that was asserted upon.
     */
    public suspend fun expectState(assertBlock: (S) -> Boolean): S {
        val event = turbine.awaitItem()
        assertTrue(
            actual = event is ViewModelEvent.State<*>,
            message = "Expected a State to be emitted, but got an Effect: ${(event as? ViewModelEvent.Effect<*>)?.effect}"
        )
        val state = (event as ViewModelEvent.State<S>).state
        assertTrue(
            actual = assertBlock(state),
            message = "State assertion failed for state: $state"
        )
        return state
    }

    /**
     * Awaits the next emission and asserts it is strictly equal to [expectedState].
     * Throws an assertion error if an Effect is emitted instead.
     */
    public suspend fun expectStateEquals(expectedState: S): S {
        val event = turbine.awaitItem()
        assertTrue(
            actual = event is ViewModelEvent.State<*>,
            message = "Expected a State to be emitted, but got an Effect: ${(event as? ViewModelEvent.Effect<*>)?.effect}"
        )
        val state = (event as ViewModelEvent.State<S>).state
        assertEquals(expectedState, state)
        return state
    }
    
    /**
     * Awaits the next emission and asserts that it is an Effect strictly equal to [expectedEffect].
     * Throws an assertion error if a State is emitted instead.
     */
    public suspend fun expectEffectEquals(expectedEffect: E): E {
        val event = turbine.awaitItem()
        assertTrue(
            actual = event is ViewModelEvent.Effect<*>,
            message = "Expected an Effect to be emitted, but got a State: ${(event as? ViewModelEvent.State<*>)?.state}"
        )
        val effect = (event as ViewModelEvent.Effect<E>).effect
        assertEquals(expectedEffect, effect)
        return effect
    }
    
    /**
     * Awaits the next emission and asserts that it is an Effect of the given class type.
     * Throws an assertion error if a State is emitted instead, or if it is the wrong Effect type.
     */
    public suspend inline fun <reified T : E> expectEffectClass(): T {
        val event = turbine.awaitItem()
        if (event !is ViewModelEvent.Effect<*>) {
            throw AssertionError("Expected an Effect to be emitted, but got a State: ${(event as? ViewModelEvent.State<*>)?.state}")
        }
        val eff = event.effect
        if (eff !is T) {
            throw AssertionError("Expected Effect to be of type ${T::class.simpleName}, but was ${eff?.let { it::class.simpleName } ?: "null"}")
        }
        return eff
    }

    /**
     * Awaits the next emission (either State or Effect).
     */
    public suspend fun awaitEvent(): ViewModelEvent<S, E> {
        return turbine.awaitItem()
    }

    /**
     * Consume and ignore the next [count] events (can be mixed combination of States and Effects).
     */
    public suspend fun skipEvents(count: Int) {
        turbine.skipItems(count)
    }

    /**
     * Cancel the flow collection and ignore any remaining events.
     */
    public suspend fun cancelAndIgnoreRemainingEvents() {
        turbine.cancelAndIgnoreRemainingEvents()
    }
}

/**
 * Starts a test block for a [BaseViewModelWithEffect], allowing dispatch of actions and exact
 * sequential assertions of BOTH [UiState] changes and [UiEffect] emissions chronologically.
 *
 * Example:
 * ```kotlin
 * viewModel.testEffects {
 *     // Initial state is emitted automatically
 *     expectState { !it.isLoading }
 *     
 *     dispatchAction(LoginAction.Login)
 *     
 *     expectState { it.isLoading }
 *     expectEffectEquals(LoginEffect.ShowToast("Success"))
 *     expectState { !it.isLoading }
 * }
 * ```
 *
 * @param timeout An optional custom timeout for the test.
 * @param name An optional name for the test, useful for debug output.
 * @param validate The test block where actions are dispatched and state/effects are asserted.
 */
public suspend fun <S : UiState, A : UiAction, E> BaseViewModelWithEffect<S, A, E>.testEffects(
    timeout: Duration? = null,
    name: String? = null,
    validate: suspend ViewModelEffectTestScope<S, A, E>.() -> Unit
) {
    val mergedFlow: Flow<ViewModelEvent<S, E>> = merge(
        this.state.asStateEvents<S, E>(),
        this.sideEffects.asEffectEvents<S, E>()
    )

    val viewModel = this
    mergedFlow.test(timeout, name) {
        val scope = ViewModelEffectTestScope<S, A, E>(
            actionHandler = { action -> viewModel.handleAction(action) },
            turbine = this
        )
        scope.validate()
        
        // After validation, ensure no unconsumed items unless explicitly ignored
        cancelAndIgnoreRemainingEvents()
    }
}
