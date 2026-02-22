# Actions Module Guide

The `actions` module extends `BaseViewModel` with utilities for dispatching multiple actions in different execution patterns — serial, parallel, chained, and batch.

## Module Overview

| Class | Purpose |
|-------|---------|
| `BaseActionsViewModel<S, A>` | ViewModel with multi-action dispatching |

## Installation

```kotlin
commonMain.dependencies {
    implementation("com.helloanwar.mvvmate:actions:<version>")
}
```

## Dispatch Patterns

### Serial Dispatch

Execute actions **one by one**, in order. Each action completes before the next starts:

```kotlin
class SetupViewModel : BaseActionsViewModel<SetupState, SetupAction>(
    initialState = SetupState()
) {
    override suspend fun onAction(action: SetupAction) {
        when (action) {
            SetupAction.RunFullSetup -> runSetup()
            SetupAction.LoadConfig -> loadConfig()
            SetupAction.SyncData -> syncData()
            SetupAction.ValidateSetup -> validate()
        }
    }

    private suspend fun runSetup() {
        // Actions execute sequentially: LoadConfig → SyncData → ValidateSetup
        dispatchActionsInSeries(
            listOf(
                SetupAction.LoadConfig,
                SetupAction.SyncData,
                SetupAction.ValidateSetup
            )
        )
    }

    private suspend fun loadConfig() {
        updateState { copy(status = "Loading configuration...") }
        delay(1000)
        updateState { copy(configLoaded = true) }
    }

    private suspend fun syncData() {
        updateState { copy(status = "Syncing data...") }
        delay(1500)
        updateState { copy(dataSynced = true) }
    }

    private suspend fun validate() {
        updateState { copy(status = "Validating...") }
        delay(500)
        updateState { copy(setupComplete = true, status = "Complete!") }
    }
}
```

**When to use:** When actions must execute in strict order — step 2 depends on step 1's side effects.

### Parallel Dispatch

Execute all actions **concurrently** and wait for all to complete:

```kotlin
private suspend fun loadDashboard() {
    // All three load simultaneously — total time = max(individual times)
    dispatchActionsInParallel(
        listOf(
            DashboardAction.LoadProfile,
            DashboardAction.LoadNotifications,
            DashboardAction.LoadRecentActivity
        )
    )
    // This line runs only after ALL three complete
    updateState { copy(dashboardReady = true) }
}
```

**When to use:** Multiple independent operations that can run simultaneously — loading multiple API endpoints for a dashboard.

### Chained Dispatch

Execute functions sequentially where each one **receives the result** of the previous:

```kotlin
private suspend fun processOrder() {
    val finalResult = dispatchChainedActions<OrderData>(
        actions = listOf(
            { _ ->
                // Step 1: Validate cart
                val cart = api.getCart()
                OrderData(cart = cart, total = cart.calculateTotal())
            },
            { previous ->
                // Step 2: Apply discounts (receives cart data)
                val discounted = api.applyDiscounts(previous!!.cart)
                previous.copy(total = discounted.total)
            },
            { previous ->
                // Step 3: Process payment (receives discounted total)
                val receipt = api.processPayment(previous!!.total)
                previous.copy(receiptId = receipt.id)
            }
        ),
        initialData = null
    )

    updateState { copy(orderId = finalResult?.receiptId) }
}
```

**When to use:** Multi-step pipelines where each step needs data from the previous one.

### Batch Dispatch

Launch all actions **concurrently without waiting** for completion:

```kotlin
private suspend fun sendAnalytics() {
    // Fire-and-forget: all launch in parallel, function returns immediately
    dispatchBatchActions(
        listOf(
            AnalyticsAction.TrackPageView,
            AnalyticsAction.SyncOfflineEvents,
            AnalyticsAction.UpdateUserProperties
        )
    )
    // Note: This still waits because coroutineScope is used internally
}
```

**When to use:** Fire-and-forget operations like analytics, logging, or cache warming where you don't need to coordinate completion.

## Comparison Table

| Method | Execution | Waits? | Data Passing | Use Case |
|--------|-----------|--------|--------------|----------|
| `dispatchActionsInSeries` | Sequential | Yes | Via state | Ordered setup steps |
| `dispatchActionsInParallel` | Concurrent | Yes, all | Via state | Dashboard loading |
| `dispatchChainedActions` | Sequential | Yes | Via return value | Data pipelines |
| `dispatchBatchActions` | Concurrent | Yes (scope) | Via state | Analytics, logging |

## Network + Actions (Combined)

The `network-actions` module combines both capabilities:

```kotlin
// Add to dependencies:
// implementation("com.helloanwar.mvvmate:network-actions:<version>")

class DashboardViewModel : BaseNetworkActionsViewModel<DashboardState, DashboardAction>(
    initialState = DashboardState()
) {
    override suspend fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.LoadAll -> loadAll()
            DashboardAction.LoadProfile -> loadProfile()
            DashboardAction.LoadFeed -> loadFeed()
        }
    }

    private suspend fun loadAll() {
        // Use BOTH dispatching patterns AND network call management
        dispatchActionsInParallel(
            listOf(
                DashboardAction.LoadProfile,
                DashboardAction.LoadFeed
            )
        )
    }

    private suspend fun loadProfile() {
        performNetworkCall<Profile>(
            partialKey = "profile",
            onSuccess = { updateState { copy(profile = it) } },
            onError = { updateState { copy(error = it) } },
            networkCall = { api.getProfile() }
        )
    }

    private suspend fun loadFeed() {
        performNetworkCallWithRetry<List<FeedItem>>(
            retries = 2,
            partialKey = "feed",
            onSuccess = { updateState { copy(feed = it) } },
            onError = { updateState { copy(error = it) } },
            networkCall = { api.getFeed() }
        )
    }

    override fun DashboardState.setPartialLoadingState(key: String) =
        copy(loadingKeys = loadingKeys + key)
    override fun DashboardState.resetPartialLoadingState(key: String) =
        copy(loadingKeys = loadingKeys - key)
}
```
