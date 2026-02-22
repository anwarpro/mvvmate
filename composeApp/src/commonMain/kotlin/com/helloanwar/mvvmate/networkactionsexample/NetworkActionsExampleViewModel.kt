package com.helloanwar.mvvmate.networkactionsexample

import com.helloanwar.mvvmate.network_actions.BaseNetworkActionsViewModel
import kotlinx.coroutines.delay

class NetworkActionsExampleViewModel :
    BaseNetworkActionsViewModel<NetworkActionsExampleState, NetworkActionsExampleAction>(
        initialState = NetworkActionsExampleState()
    ) {

    override suspend fun onAction(action: NetworkActionsExampleAction) {
        when (action) {
            NetworkActionsExampleAction.FetchAllPosts -> fetchAllPostsParallel()
            NetworkActionsExampleAction.FetchPostsSeries -> fetchPostsSeries()
            NetworkActionsExampleAction.FetchWithRetryAndBatch -> fetchWithRetryAndBatch()
            NetworkActionsExampleAction.CancelAll -> {
                cancelNetworkCall("post-fetch")
                addLog("🛑 Cancelled all ongoing requests")
            }

            NetworkActionsExampleAction.ClearAll -> updateState {
                copy(posts = emptyList(), error = null, logs = emptyList())
            }

            is NetworkActionsExampleAction.AddLog -> addLog(action.message)
            is NetworkActionsExampleAction.AddPost -> updateState {
                copy(posts = posts + action.post)
            }
        }
    }

    private suspend fun fetchAllPostsParallel() {
        addLog("🚀 Fetching 3 posts in parallel using dispatchActionsInParallel + performNetworkCall...")
        updateState { copy(posts = emptyList()) }

        // Dispatch multiple network fetches in parallel via action dispatching
        dispatchActionsInParallel(
            listOf(
                NetworkActionsExampleAction.AddLog("  ⏳ Fetching post 1..."),
                NetworkActionsExampleAction.AddLog("  ⏳ Fetching post 2..."),
                NetworkActionsExampleAction.AddLog("  ⏳ Fetching post 3...")
            )
        )

        // Then perform the actual network calls
        performNetworkCall<List<String>>(
            isGlobal = true,
            onSuccess = { posts ->
                posts.forEach { post ->
                    updateState { copy(posts = this.posts + post) }
                }
                addLog("✅ All posts fetched successfully!")
            },
            onError = { error ->
                addLog("❌ Error fetching posts: ${error.message}")
                updateState { copy(error = error.message) }
            },
            networkCall = {
                delay(2000) // Simulate network delay
                listOf(
                    "📝 Post 1: Getting Started with MVVMate",
                    "📝 Post 2: Advanced State Management",
                    "📝 Post 3: Network Patterns in KMP"
                )
            }
        )
    }

    private suspend fun fetchPostsSeries() {
        addLog("📋 Fetching posts in series using dispatchActionsInSeries + performNetworkCallWithRetry...")
        updateState { copy(posts = emptyList()) }

        // Use chained actions to fetch posts sequentially with network retry
        val titles = listOf("Kotlin Basics", "Compose Layouts", "Multiplatform Tips")

        for ((index, title) in titles.withIndex()) {
            addLog("  ⏳ Fetching post ${index + 1}: $title...")

            performNetworkCallWithRetry<String>(
                retries = 2,
                initialDelay = 300L,
                isGlobal = true,
                onSuccess = { post ->
                    updateState { copy(posts = posts + post) }
                    addLog("  ✅ Post ${index + 1} loaded")
                },
                onError = { error ->
                    addLog("  ❌ Failed to load post ${index + 1}: ${error.message}")
                },
                networkCall = {
                    delay(800)
                    "📝 Post ${index + 1}: $title — Full content loaded with retry support"
                }
            )
        }

        addLog("✅ Series fetch complete!")
    }

    private suspend fun fetchWithRetryAndBatch() {
        addLog("🔄 Combined: Retry network call + batch background actions...")

        // Perform a network call with retry
        performNetworkCallWithRetry<String>(
            retries = 3,
            initialDelay = 500L,
            maxDelay = 2000L,
            isGlobal = true,
            onSuccess = { result ->
                addLog("✅ Network call succeeded: $result")
                updateState { copy(posts = posts + result) }
            },
            onError = { error ->
                addLog("❌ Network call failed after retries: ${error.message}")
                updateState { copy(error = error.message) }
            },
            networkCall = {
                delay(1000)
                "📝 Premium content loaded (with retry protection)"
            }
        )

        // Then batch fire-and-forget background tasks
        addLog("⚡ Dispatching batch background tasks...")
        dispatchBatchActions(
            listOf(
                NetworkActionsExampleAction.AddLog("  [Batch] 📊 Analytics event recorded"),
                NetworkActionsExampleAction.AddLog("  [Batch] 💾 Cache updated"),
                NetworkActionsExampleAction.AddLog("  [Batch] 🔔 Push notification scheduled")
            )
        )

        addLog("✅ Retry + Batch demo complete!")
    }

    private fun addLog(message: String) {
        updateState { copy(logs = logs + message) }
    }

    // Loading state overrides
    override fun NetworkActionsExampleState.setGlobalLoadingState(): NetworkActionsExampleState =
        copy(isLoading = true)

    override fun NetworkActionsExampleState.resetGlobalLoadingState(): NetworkActionsExampleState =
        copy(isLoading = false)

    override fun NetworkActionsExampleState.setPartialLoadingState(key: String): NetworkActionsExampleState =
        copy(loadingKeys = loadingKeys + key)

    override fun NetworkActionsExampleState.resetPartialLoadingState(key: String): NetworkActionsExampleState =
        copy(loadingKeys = loadingKeys - key)
}
