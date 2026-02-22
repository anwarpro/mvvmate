package com.helloanwar.mvvmate.networkexample

import com.helloanwar.mvvmate.network.BaseNetworkViewModel
import kotlinx.coroutines.delay

class NetworkExampleViewModel : BaseNetworkViewModel<NetworkExampleState, NetworkExampleAction>(
    initialState = NetworkExampleState()
) {

    override suspend fun onAction(action: NetworkExampleAction) {
        when (action) {
            NetworkExampleAction.FetchData -> fetchData()
            NetworkExampleAction.FetchWithRetry -> fetchWithRetry()
            NetworkExampleAction.FetchWithTimeout -> fetchWithTimeout()
            NetworkExampleAction.FetchWithCancellation -> fetchWithCancellation()
            NetworkExampleAction.CancelFetch -> cancelNetworkCall("search")
            NetworkExampleAction.ClearAll -> updateState {
                copy(data = null, error = null, logs = emptyList())
            }
        }
    }

    private suspend fun fetchData() {
        addLog("⏳ Starting basic network call...")
        performNetworkCall<String>(
            isGlobal = true,
            onSuccess = { result ->
                addLog("✅ Success: $result")
                updateState { copy(data = result, error = null) }
            },
            onError = { error ->
                addLog("❌ Error: $error")
                updateState { copy(error = error) }
            },
            networkCall = {
                delay(1500) // Simulated network delay
                "User data loaded successfully from API"
            }
        )
    }

    private suspend fun fetchWithRetry() {
        addLog("🔄 Starting network call with retry (3 attempts)...")
        var attempt = 0
        performNetworkCallWithRetry<String>(
            retries = 3,
            initialDelay = 500L,
            maxDelay = 2000L,
            isGlobal = true,
            onSuccess = { result ->
                addLog("✅ Retry success: $result")
                updateState { copy(data = result, error = null) }
            },
            onError = { error ->
                addLog("❌ All retries failed: $error")
                updateState { copy(error = error) }
            },
            networkCall = {
                attempt++
                addLog("  Attempt #$attempt...")
                delay(800)
                if (attempt < 3) {
                    throw Exception("Server error (attempt $attempt)")
                }
                "Data retrieved after $attempt attempts"
            }
        )
    }

    private suspend fun fetchWithTimeout() {
        addLog("⏱️ Starting network call with 2s timeout...")
        performNetworkCallWithTimeout<String>(
            timeoutMillis = 2000L,
            isGlobal = true,
            onSuccess = { result ->
                addLog("✅ Timeout success: $result")
                updateState { copy(data = result, error = null) }
            },
            onError = { error ->
                addLog("❌ Timeout error: $error")
                updateState { copy(error = error) }
            },
            networkCall = {
                delay(3000) // This will exceed the 2s timeout
                "This won't be reached"
            }
        )
    }

    private fun fetchWithCancellation() {
        addLog("🔍 Starting cancellable search call (tag: 'search')...")
        performNetworkCallWithCancellation<String>(
            tag = "search",
            partialKey = "search",
            onSuccess = { result ->
                addLog("✅ Search success: $result")
                updateState { copy(data = result, error = null) }
            },
            onError = { error ->
                addLog("❌ Search error: $error")
                updateState { copy(error = error) }
            },
            networkCall = {
                delay(3000) // Long running operation
                "Search results for query"
            }
        )
    }

    private fun addLog(message: String) {
        updateState { copy(logs = logs + message) }
    }

    // Override loading state management
    override fun NetworkExampleState.setGlobalLoadingState(): NetworkExampleState =
        copy(isLoading = true)

    override fun NetworkExampleState.resetGlobalLoadingState(): NetworkExampleState =
        copy(isLoading = false)

    override fun NetworkExampleState.setPartialLoadingState(key: String): NetworkExampleState =
        copy(loadingKeys = loadingKeys + key)

    override fun NetworkExampleState.resetPartialLoadingState(key: String): NetworkExampleState =
        copy(loadingKeys = loadingKeys - key)
}
