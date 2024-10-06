package com.helloanwar.mvvmate.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Base class for ViewModels that manages UI state, handles user actions, and emits side effects.
 *
 * This class extends [BaseViewModel] by adding support for emitting side effects
 * via a [SharedFlow]. Side effects are typically used for actions that should not
 * directly mutate the UI state but trigger external events, such as navigation or showing a dialog.
 *
 * @param S The type of UI state that this ViewModel manages. It should implement [UiState].
 * @param A The type of user actions (intents) that this ViewModel responds to. It should implement [UiAction].
 * @param E The type of side effects that this ViewModel emits.
 * @property initialState The initial state of the ViewModel when it is created.
 */
abstract class BaseViewModelWithEffect<S : UiState, A : UiAction, E>(
    initialState: S
) : BaseViewModel<S, A>(initialState) {

    /**
     * A [SharedFlow] for emitting one-time side effects such as navigation or showing dialogs.
     */
    private val _sideEffects = MutableSharedFlow<E>()
    val sideEffects: SharedFlow<E> get() = _sideEffects

    /**
     * Emit a side effect to be handled by the UI.
     *
     * @param effect The side effect to emit.
     */
    protected suspend fun emitSideEffect(effect: E) {
        _sideEffects.emit(effect)
    }
}
