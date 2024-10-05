package com.helloanwar.mvvmate.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

abstract class BaseViewModelWithEffect<S : UiState, A : UiAction, E>(
    initialState: S
) : BaseViewModel<S, A>(initialState) {

    private val _sideEffects = MutableSharedFlow<E>()
    val sideEffects: SharedFlow<E> get() = _sideEffects

    protected suspend fun emitSideEffect(effect: E) {
        _sideEffects.emit(effect)
    }
}
