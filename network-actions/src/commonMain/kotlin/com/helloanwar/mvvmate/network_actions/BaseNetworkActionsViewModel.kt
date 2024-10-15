package com.helloanwar.mvvmate.network_actions

import androidx.lifecycle.viewModelScope
import com.helloanwar.mvvmate.actions.BaseActionsViewModel
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

abstract class BaseNetworkActionsViewModel<S : UiState, A : UiAction>(
    initialState: S
) : BaseActionsViewModel<S, A>(initialState) {

    private val ongoingJobs = mutableMapOf<String, Job>() // For tracking cancellable jobs

    // Perform network call with basic support (for global/partial loading)
    protected suspend fun <T> performNetworkCall(
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) {
        try {
            setLoadingState(isGlobal, partialKey)
            val result = networkCall()
            resetLoadingState(isGlobal, partialKey)
            onSuccess(result)
        } catch (e: Exception) {
            resetLoadingState(isGlobal, partialKey)
            onError(e.message ?: "Unknown Error")
        }
    }

    // Perform network call with retry
    protected suspend fun <T> performNetworkCallWithRetry(
        retries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 4000L,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) {
        var currentDelay = initialDelay
        repeat(retries) { attempt ->
            try {
                setLoadingState(isGlobal, partialKey)
                val result = networkCall()
                resetLoadingState(isGlobal, partialKey)
                onSuccess(result)
                return
            } catch (e: Exception) {
                if (attempt == retries - 1) {
                    resetLoadingState(isGlobal, partialKey)
                    onError(e.message ?: "Unknown Error")
                } else {
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                }
            }
        }
    }

    // Perform network call with timeout
    protected suspend fun <T> performNetworkCallWithTimeout(
        timeoutMillis: Long = 5000L,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) {
        try {
            setLoadingState(isGlobal, partialKey)
            val result = withTimeout(timeoutMillis) { networkCall() }
            resetLoadingState(isGlobal, partialKey)
            onSuccess(result)
        } catch (e: TimeoutCancellationException) {
            resetLoadingState(isGlobal, partialKey)
            onError("Operation timed out")
        } catch (e: Exception) {
            resetLoadingState(isGlobal, partialKey)
            onError(e.message ?: "Unknown Error")
        }
    }

    // Perform network call with cancellation support
    protected fun <T> performNetworkCallWithCancellation(
        tag: String,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) {
        // Cancel any ongoing job with the same tag
        ongoingJobs[tag]?.cancel()

        val job = viewModelScope.launch {
            try {
                setLoadingState(isGlobal, partialKey)
                val result = networkCall()
                resetLoadingState(isGlobal, partialKey)
                onSuccess(result)
            } catch (e: Exception) {
                resetLoadingState(isGlobal, partialKey)
                onError(e.message ?: "Unknown Error")
            } finally {
                ongoingJobs.remove(tag)
            }
        }

        ongoingJobs[tag] = job
    }

    // Helper method to cancel a specific network call
    fun cancelNetworkCall(tag: String) {
        ongoingJobs[tag]?.cancel()
        ongoingJobs.remove(tag)
    }

    // Placeholder methods for setting/resetting loading state
    private fun setLoadingState(isGlobal: Boolean, partialKey: String?) {
        if (isGlobal) {
            updateState { setGlobalLoadingState() }
        } else if (partialKey != null) {
            updateState { setPartialLoadingState(partialKey) }
        }
    }

    private fun resetLoadingState(isGlobal: Boolean, partialKey: String?) {
        if (isGlobal) {
            updateState { resetGlobalLoadingState() }
        } else if (partialKey != null) {
            updateState { resetPartialLoadingState(partialKey) }
        }
    }

    // Default implementations for loading state can be overridden by subclasses
    protected open fun S.setGlobalLoadingState(): S = this
    protected open fun S.resetGlobalLoadingState(): S = this
    protected open fun S.setPartialLoadingState(key: String): S = this
    protected open fun S.resetPartialLoadingState(key: String): S = this
}