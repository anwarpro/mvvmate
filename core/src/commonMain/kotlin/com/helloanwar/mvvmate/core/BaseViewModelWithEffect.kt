package com.helloanwar.mvvmate.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Base class for ViewModels that manages UI state, handles user actions, and emits side effects.
 *
 * This class extends [BaseViewModel] by adding support for emitting side effects
 * via a [Channel] exposed as a [Flow]. Side effects are buffered and guaranteed
 * to be delivered even if no collector is active at the time of emission.
 *
 * Side effects are typically used for actions that should not directly mutate the
 * UI state but trigger external events, such as navigation or showing a dialog.
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
     * Buffered channel for side effects. Using [Channel.BUFFERED] ensures effects
     * are not lost when no collector is active — they queue up and are delivered
     * when a collector subscribes.
     */
    private val _sideEffects = Channel<E>(Channel.BUFFERED)

    /**
     * Flow of one-time side effects such as navigation or showing dialogs.
     * Collect this in a `LaunchedEffect` block in Compose.
     */
    val sideEffects: Flow<E> = _sideEffects.receiveAsFlow()

    /**
     * Emit a side effect to be handled by the UI.
     *
     * @param effect The side effect to emit.
     */
    protected suspend fun emitSideEffect(effect: E) {
        MvvMate.logger.logEffect(logTag, effect as Any)
        _sideEffects.send(effect)
    }
}
