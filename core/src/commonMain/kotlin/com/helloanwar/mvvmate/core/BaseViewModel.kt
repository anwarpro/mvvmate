package com.helloanwar.mvvmate.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for ViewModels that manages UI state and handles user actions.
 *
 * This class holds the common logic for state management using [StateFlow] and
 * provides a mechanism to handle actions asynchronously using Kotlin coroutines.
 *
 * @param S The type of UI state that this ViewModel manages. It should implement [UiState].
 * @param A The type of user actions (intents) that this ViewModel responds to. It should implement [UiAction].
 * @property initialState The initial state of the ViewModel when it is created.
 */
abstract class BaseViewModel<S : UiState, A : UiAction>(
    initialState: S
) : ViewModel() {

    /**
     * StateFlow that represents the current UI state.
     */
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> get() = _state

    /**
     * Method to update the current state of the UI.
     *
     * @param reducer A lambda that defines how to modify the state.
     */
    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    /**
     * Handles a user action and updates the state accordingly using the provided [onAction] method.
     * Uses [viewModelScope] to ensure coroutines are cancelled when the ViewModel is cleared.
     *
     * @param action The user action to handle.
     */
    fun handleAction(action: A) {
        viewModelScope.launch {
            try {
                onAction(action)
            } catch (e: CancellationException) {
                throw e // Never swallow CancellationException
            } catch (e: Exception) {
                onError(action, e)
            }
        }
    }

    /**
     * Abstract method to be implemented by subclasses to handle the specific action logic.
     *
     * @param action The user action to be handled.
     */
    abstract suspend fun onAction(action: A)

    /**
     * Called when [onAction] throws an exception (other than [CancellationException]).
     * Override to provide custom error handling.
     *
     * @param action The action that was being processed when the error occurred.
     * @param error The exception that was thrown.
     */
    protected open fun onError(action: A, error: Exception) {
        // Default: no-op. Subclasses can override for custom error handling.
    }
}
