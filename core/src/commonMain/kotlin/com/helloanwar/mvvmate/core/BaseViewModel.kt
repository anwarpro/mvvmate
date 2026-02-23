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
 * Integrates with [MvvMate.logger] for automatic action, state, and error logging.
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
     * Name used in log messages. Defaults to the class simple name.
     */
    protected open val logTag: String
        get() = this::class.simpleName ?: "ViewModel"

    /**
     * Method to update the current state of the UI.
     *
     * @param reducer A lambda that defines how to modify the state.
     */
    protected fun updateState(reducer: S.() -> S) {
        val oldState = _state.value
        _state.update { it.reducer() }
        val newState = _state.value
        if (MvvMate.isDebug && oldState != newState) {
            MvvMate.logger.logStateChange(logTag, oldState, newState)
        }
    }

    /**
     * Handles a user action and updates the state accordingly using the provided [onAction] method.
     * Uses [viewModelScope] to ensure coroutines are cancelled when the ViewModel is cleared.
     *
     * @param action The user action to handle.
     */
    fun handleAction(action: A) {
        // Auto-register for time-travel and action injection on first action
        if (MvvMate.isDebug && logTag !in MvvMate.debugBridge) {
            MvvMate.debugBridge[logTag] = object : MvvMate.DebugBridge {
                override fun restoreState(state: UiState) = debugForceSetState(state)
                override fun injectAction(payload: String) {
                    val actionToInject = mapDebugAction(payload)
                    if (actionToInject != null) {
                        handleAction(actionToInject)
                    }
                }
            }
        }
        MvvMate.logger.logAction(logTag, action)
        viewModelScope.launch {
            try {
                onAction(action)
            } catch (e: CancellationException) {
                throw e // Never swallow CancellationException
            } catch (e: Exception) {
                MvvMate.logger.logError(logTag, e, "onAction(${action::class.simpleName})")
                onError(action, e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        MvvMate.debugBridge.remove(logTag)
    }

    /**
     * Maps a string payload (from the remote debugger) to an actual [UiAction].
     *
     * Override this in your ViewModel to support automatic action injection
     * from the Studio Plugin's Action Injector.
     *
     * @param payload The string payload sent from the IDE.
     * @return The [UiAction] to handle, or null if unknown.
     */
    protected open fun mapDebugAction(payload: String): A? = null

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

    /**
     * Force-set the state for debug/time-travel purposes.
     *
     * This allows the remote debugger to restore a previous state snapshot.
     * Only works when [MvvMate.isDebug] is true.
     *
     * @param state The state object to restore, must be the same type [S].
     */
    @Suppress("UNCHECKED_CAST")
    fun debugForceSetState(state: UiState) {
        if (!MvvMate.isDebug) return
        try {
            _state.value = state as S
        } catch (_: ClassCastException) {
            // Type mismatch — ignore silently
        }
    }
}
