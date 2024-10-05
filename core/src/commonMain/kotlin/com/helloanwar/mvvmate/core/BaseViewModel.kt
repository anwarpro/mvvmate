package com.helloanwar.mvvmate.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

abstract class BaseViewModel<S : UiState, A : UiAction>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> get() = _state

    // Expose a method to update the state
    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    // Handle intents or actions
    fun handleAction(action: A) {
        CoroutineScope(Dispatchers.Main).launch {
            onAction(action)
        }
    }

    // Abstract method to handle actions
    abstract suspend fun onAction(action: A)
}
