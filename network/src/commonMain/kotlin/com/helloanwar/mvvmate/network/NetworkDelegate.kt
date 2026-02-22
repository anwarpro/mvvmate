package com.helloanwar.mvvmate.network

import androidx.lifecycle.viewModelScope
import com.helloanwar.mvvmate.core.BaseViewModel
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Internal delegate that contains all shared network call logic.
 * Used by both [BaseNetworkViewModel] and consumed by `network-actions` module
 * to avoid code duplication.
 *
 * @param S The type of UI state.
 * @param updateState Function to update the ViewModel's state.
 * @param launchInScope Function to launch a coroutine in viewModelScope.
 */
class NetworkDelegate<S : UiState>(
    private val updateState: (S.() -> S) -> Unit,
    private val launchInScope: (suspend () -> Unit) -> Job
) {

    private val ongoingJobs = mutableMapOf<String, Job>()

    // Loading state callbacks — must be set by the owning ViewModel
    var setGlobalLoadingState: S.() -> S = { this }
    var resetGlobalLoadingState: S.() -> S = { this }
    var setPartialLoadingState: S.(key: String) -> S = { this }
    var resetPartialLoadingState: S.(key: String) -> S = { this }

    /**
     * Perform network call with basic support (for global/partial loading).
     */
    suspend fun <T> performNetworkCall(
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
        } catch (e: CancellationException) {
            resetLoadingState(isGlobal, partialKey)
            throw e
        } catch (e: Exception) {
            resetLoadingState(isGlobal, partialKey)
            onError(e.message ?: "Unknown Error")
        }
    }

    /**
     * Perform network call with retry and exponential backoff.
     */
    suspend fun <T> performNetworkCallWithRetry(
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
            } catch (e: CancellationException) {
                resetLoadingState(isGlobal, partialKey)
                throw e
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

    /**
     * Perform network call with timeout.
     */
    suspend fun <T> performNetworkCallWithTimeout(
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

    /**
     * Perform network call with cancellation support.
     * Uses the owning ViewModel's scope via [launchInScope].
     */
    fun <T> performNetworkCallWithCancellation(
        tag: String,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
        networkCall: suspend () -> T
    ) {
        // Cancel any ongoing job with the same tag
        ongoingJobs[tag]?.cancel()

        val job = launchInScope {
            try {
                setLoadingState(isGlobal, partialKey)
                val result = networkCall()
                resetLoadingState(isGlobal, partialKey)
                onSuccess(result)
            } catch (e: CancellationException) {
                resetLoadingState(isGlobal, partialKey)
                throw e
            } catch (e: Exception) {
                resetLoadingState(isGlobal, partialKey)
                onError(e.message ?: "Unknown Error")
            } finally {
                ongoingJobs.remove(tag)
            }
        }

        ongoingJobs[tag] = job
    }

    /**
     * Cancel a specific network call by tag.
     */
    fun cancelNetworkCall(tag: String) {
        ongoingJobs[tag]?.cancel()
        ongoingJobs.remove(tag)
    }

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
}
