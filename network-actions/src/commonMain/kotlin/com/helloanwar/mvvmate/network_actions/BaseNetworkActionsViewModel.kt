package com.helloanwar.mvvmate.network_actions

import androidx.lifecycle.viewModelScope
import com.helloanwar.mvvmate.actions.BaseActionsViewModel
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState
import com.helloanwar.mvvmate.network.NetworkDelegate
import kotlinx.coroutines.launch

/**
 * Base class that combines action dispatching capabilities from [BaseActionsViewModel]
 * with network call management from [NetworkDelegate].
 *
 * This eliminates the need to duplicate network logic — all network methods
 * (basic, retry, timeout, cancellation) are provided by the shared [NetworkDelegate].
 *
 * @param S The type of UI state.
 * @param A The type of user actions.
 * @property initialState The initial state of the ViewModel.
 */
abstract class BaseNetworkActionsViewModel<S : UiState, A : UiAction>(
    initialState: S
) : BaseActionsViewModel<S, A>(initialState) {

    /**
     * The network delegate that provides all network call capabilities.
     */
    protected val networkDelegate = NetworkDelegate<S>(
        updateState = { reducer -> updateState(reducer) },
        launchInScope = { block -> viewModelScope.launch { block() } }
    )

    // --- Convenience wrappers that delegate to networkDelegate ---

    protected suspend fun <T> performNetworkCall(
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) = networkDelegate.performNetworkCall(isGlobal, partialKey, onSuccess, onError, networkCall)

    protected suspend fun <T> performNetworkCallWithRetry(
        retries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 4000L,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) = networkDelegate.performNetworkCallWithRetry(
        retries, initialDelay, maxDelay, isGlobal, partialKey, onSuccess, onError, networkCall
    )

    protected suspend fun <T> performNetworkCallWithTimeout(
        timeoutMillis: Long = 5000L,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) = networkDelegate.performNetworkCallWithTimeout(
        timeoutMillis, isGlobal, partialKey, onSuccess, onError, networkCall
    )

    protected fun <T> performNetworkCallWithCancellation(
        tag: String,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) = networkDelegate.performNetworkCallWithCancellation(
        tag, isGlobal, partialKey, onSuccess, onError, networkCall
    )

    fun cancelNetworkCall(tag: String) = networkDelegate.cancelNetworkCall(tag)

    // --- Loading state overrides — wire through to delegate ---

    protected open fun S.setGlobalLoadingState(): S = this
    protected open fun S.resetGlobalLoadingState(): S = this
    protected open fun S.setPartialLoadingState(key: String): S = this
    protected open fun S.resetPartialLoadingState(key: String): S = this

    init {
        @Suppress("LeakingThis")
        networkDelegate.setGlobalLoadingState = { setGlobalLoadingState() }
        @Suppress("LeakingThis")
        networkDelegate.resetGlobalLoadingState = { resetGlobalLoadingState() }
        @Suppress("LeakingThis")
        networkDelegate.setPartialLoadingState = { key -> setPartialLoadingState(key) }
        @Suppress("LeakingThis")
        networkDelegate.resetPartialLoadingState = { key -> resetPartialLoadingState(key) }
    }
}