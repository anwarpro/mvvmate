package com.helloanwar.mvvmate.network

import com.helloanwar.mvvmate.core.AppError
import com.helloanwar.mvvmate.core.MvvMate
import com.helloanwar.mvvmate.core.MvvMateLogger
import com.helloanwar.mvvmate.core.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Internal delegate that contains all shared network call logic.
 * Used by both [BaseNetworkViewModel] and consumed by `network-actions` module
 * to avoid code duplication.
 *
 * Thread-safe: uses [Mutex] to protect concurrent access to [ongoingJobs].
 *
 * @param S The type of UI state.
 * @param updateState Function to update the ViewModel's state.
 * @param launchInScope Function to launch a coroutine in viewModelScope.
 */
class NetworkDelegate<S : UiState>(
    private val updateState: (S.() -> S) -> Unit,
    private val launchInScope: (suspend () -> Unit) -> Job
) {

    private val jobsMutex = Mutex()
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
        onError: (AppError) -> Unit,
        networkCall: suspend () -> T
    ) {
        val tag = partialKey ?: "global"
        MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.START)
        try {
            setLoadingState(isGlobal, partialKey)
            val result = networkCall()
            resetLoadingState(isGlobal, partialKey)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.SUCCESS)
            onSuccess(result)
        } catch (e: CancellationException) {
            resetLoadingState(isGlobal, partialKey)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.CANCEL)
            throw e
        } catch (e: Exception) {
            resetLoadingState(isGlobal, partialKey)
            val appError = AppError.from(e)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.FAILURE, appError.message)
            onError(appError)
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
        onError: (AppError) -> Unit,
        networkCall: suspend () -> T
    ) {
        val tag = partialKey ?: "global"
        var currentDelay = initialDelay
        MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.START, "with $retries retries")
        repeat(retries) { attempt ->
            try {
                setLoadingState(isGlobal, partialKey)
                val result = networkCall()
                resetLoadingState(isGlobal, partialKey)
                MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.SUCCESS, "attempt ${attempt + 1}")
                onSuccess(result)
                return
            } catch (e: CancellationException) {
                resetLoadingState(isGlobal, partialKey)
                MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.CANCEL)
                throw e
            } catch (e: Exception) {
                if (attempt == retries - 1) {
                    resetLoadingState(isGlobal, partialKey)
                    val appError = AppError.from(e)
                    MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.FAILURE, "all $retries attempts failed")
                    onError(appError)
                } else {
                    MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.RETRY, "attempt ${attempt + 1}, retrying in ${currentDelay}ms")
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
        onError: (AppError) -> Unit,
        networkCall: suspend () -> T
    ) {
        val tag = partialKey ?: "global"
        MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.START, "timeout=${timeoutMillis}ms")
        try {
            setLoadingState(isGlobal, partialKey)
            val result = withTimeout(timeoutMillis) { networkCall() }
            resetLoadingState(isGlobal, partialKey)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.SUCCESS)
            onSuccess(result)
        } catch (e: TimeoutCancellationException) {
            resetLoadingState(isGlobal, partialKey)
            val appError = AppError.Timeout(message = "Operation timed out", durationMs = timeoutMillis, cause = e)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.TIMEOUT, "${timeoutMillis}ms")
            onError(appError)
        } catch (e: Exception) {
            resetLoadingState(isGlobal, partialKey)
            val appError = AppError.from(e)
            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.FAILURE, appError.message)
            onError(appError)
        }
    }

    /**
     * Perform network call with cancellation support.
     * Uses the owning ViewModel's scope via [launchInScope].
     *
     * Thread-safe: uses [Mutex] to protect [ongoingJobs] map access.
     */
    fun <T> performNetworkCallWithCancellation(
        tag: String,
        isGlobal: Boolean = false,
        partialKey: String? = null,
        onSuccess: (T) -> Unit,
        onError: (AppError) -> Unit,
        networkCall: suspend () -> T
    ) {
        val job = launchInScope {
            // Cancel any ongoing job with the same tag (thread-safe)
            jobsMutex.withLock {
                ongoingJobs[tag]?.cancel()
            }

            MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.START, "cancellable")
            try {
                setLoadingState(isGlobal, partialKey)
                val result = networkCall()
                resetLoadingState(isGlobal, partialKey)
                MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.SUCCESS)
                onSuccess(result)
            } catch (e: CancellationException) {
                resetLoadingState(isGlobal, partialKey)
                MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.CANCEL)
                throw e
            } catch (e: Exception) {
                resetLoadingState(isGlobal, partialKey)
                val appError = AppError.from(e)
                MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.FAILURE, appError.message)
                onError(appError)
            } finally {
                jobsMutex.withLock {
                    ongoingJobs.remove(tag)
                }
            }
        }

        // Also needs mutex for storing the job
        launchInScope {
            jobsMutex.withLock {
                ongoingJobs[tag] = job
            }
        }
    }

    /**
     * Cancel a specific network call by tag.
     * Note: This is called from the main thread, so we use non-suspending cancel.
     */
    fun cancelNetworkCall(tag: String) {
        // Job.cancel() is thread-safe, and we do a best-effort removal
        ongoingJobs[tag]?.cancel()
        ongoingJobs.remove(tag)
        MvvMate.logger.logNetwork(tag, MvvMateLogger.NetworkPhase.CANCEL, "manual")
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
