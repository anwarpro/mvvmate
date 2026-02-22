# Network Module Guide

The `network` module extends `BaseViewModel` with built-in patterns for network call management — loading states, retry with backoff, timeouts, cancellation, and **typed error handling** via `AppError`.

## Module Overview

| Class | Purpose |
|-------|---------|
| `BaseNetworkViewModel<S, A>` | ViewModel with network call helpers |
| `NetworkDelegate<S>` | Reusable network logic (thread-safe, used internally) |

## Installation

```kotlin
commonMain.dependencies {
    implementation("com.helloanwar.mvvmate:network:<version>")
}
```

> The `network` module automatically includes the `core` module as a dependency.

## Defining Contracts

Network-heavy screens typically need loading and error fields in state:

```kotlin
data class UsersState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null,
    val loadingKeys: Set<String> = emptySet() // For partial loading
) : UiState

sealed interface UsersAction : UiAction {
    data object FetchUsers : UsersAction
    data class SearchUsers(val query: String) : UsersAction
    data object CancelSearch : UsersAction
    data object RetryFetch : UsersAction
}
```

## Basic Network Call

The simplest way to wrap an async operation with automatic loading state management:

```kotlin
class UsersViewModel : BaseNetworkViewModel<UsersState, UsersAction>(
    initialState = UsersState()
) {
    override suspend fun onAction(action: UsersAction) {
        when (action) {
            UsersAction.FetchUsers -> fetchUsers()
            // ...
        }
    }

    private suspend fun fetchUsers() {
        performNetworkCall<List<User>>(
            isGlobal = true, // Shows global loading indicator
            onSuccess = { users ->
                updateState { copy(users = users, error = null) }
            },
            onError = { error ->
                updateState { copy(error = error.message) }
            },
            networkCall = {
                api.getUsers() // Your actual API call
            }
        )
    }

    // Wire up loading state management
    override fun UsersState.setGlobalLoadingState() = copy(isLoading = true)
    override fun UsersState.resetGlobalLoadingState() = copy(isLoading = false)
}
```

## Network Call with Retry

Automatically retries failed requests with exponential backoff:

```kotlin
private suspend fun fetchUsersWithRetry() {
    performNetworkCallWithRetry<List<User>>(
        retries = 3,            // Number of attempts
        initialDelay = 1000L,   // 1 second initial delay
        maxDelay = 4000L,       // Cap delay at 4 seconds
        isGlobal = true,
        onSuccess = { users ->
            updateState { copy(users = users, error = null) }
        },
        onError = { error ->
            // Called only after ALL retries have failed
            updateState { copy(error = "Failed after 3 attempts: ${error.message}") }
        },
        networkCall = {
            api.getUsers()
        }
    )
}
```

**Backoff schedule** (default settings):
| Attempt | Delay Before Retry |
|---------|-------------------|
| 1st failure | 1,000ms |
| 2nd failure | 2,000ms |
| 3rd failure | 4,000ms (capped) |

## Network Call with Timeout

Set a maximum duration for operations:

```kotlin
private suspend fun fetchWithTimeout() {
    performNetworkCallWithTimeout<List<User>>(
        timeoutMillis = 5000L, // 5 second timeout
        isGlobal = true,
        onSuccess = { users ->
            updateState { copy(users = users) }
        },
        onError = { error ->
            // error will be AppError.Timeout for timeouts
            updateState { copy(error = error.message) }
        },
        networkCall = {
            api.getUsers()
        }
    )
}
```

## Network Call with Cancellation

For operations that should be cancelled when a new request comes in (e.g., search-as-you-type):

```kotlin
private fun searchUsers(query: String) {
    // Any previous search with the same tag is automatically cancelled
    performNetworkCallWithCancellation<List<User>>(
        tag = "user-search",            // Unique identifier for this operation
        partialKey = "search",          // Partial loading key
        onSuccess = { users ->
            updateState { copy(users = users) }
        },
        onError = { error ->
            updateState { copy(error = error.message) }
        },
        networkCall = {
            api.searchUsers(query)
        }
    )
}

// Cancel explicitly from an action
override suspend fun onAction(action: UsersAction) {
    when (action) {
        UsersAction.CancelSearch -> cancelNetworkCall("user-search")
        // ...
    }
}
```

## Loading State Management

MVVMate supports two types of loading indicators:

### Global Loading

One loading state for the entire screen:

```kotlin
override fun UsersState.setGlobalLoadingState() = copy(isLoading = true)
override fun UsersState.resetGlobalLoadingState() = copy(isLoading = false)
```

### Partial Loading

Multiple independent loading states — useful when multiple sections load independently:

```kotlin
override fun UsersState.setPartialLoadingState(key: String) =
    copy(loadingKeys = loadingKeys + key)

override fun UsersState.resetPartialLoadingState(key: String) =
    copy(loadingKeys = loadingKeys - key)
```

Usage in ViewModel:

```kotlin
private suspend fun loadUserDetails(userId: String) {
    performNetworkCall<UserDetail>(
        isGlobal = false,
        partialKey = "user-$userId", // Unique key per user
        onSuccess = { detail -> /* ... */ },
        onError = { error -> /* ... */ },
        networkCall = { api.getUserDetail(userId) }
    )
}
```

Usage in Compose:

```kotlin
@Composable
fun UserItem(userId: String, state: UsersState) {
    val isThisUserLoading = "user-$userId" in state.loadingKeys

    if (isThisUserLoading) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
    }
    // ... render user
}
```

## Complete Example

```kotlin
class UsersViewModel : BaseNetworkViewModel<UsersState, UsersAction>(
    initialState = UsersState()
) {
    override suspend fun onAction(action: UsersAction) {
        when (action) {
            UsersAction.FetchUsers -> fetchUsers()
            is UsersAction.SearchUsers -> searchUsers(action.query)
            UsersAction.CancelSearch -> cancelNetworkCall("search")
            UsersAction.RetryFetch -> fetchUsersWithRetry()
        }
    }

    private suspend fun fetchUsers() {
        performNetworkCall<List<User>>(
            isGlobal = true,
            onSuccess = { updateState { copy(users = it, error = null) } },
            onError = { updateState { copy(error = it.message) } },
            networkCall = { api.getUsers() }
        )
    }

    private suspend fun fetchUsersWithRetry() {
        performNetworkCallWithRetry<List<User>>(
            retries = 3,
            isGlobal = true,
            onSuccess = { updateState { copy(users = it, error = null) } },
            onError = { updateState { copy(error = it.message) } },
            networkCall = { api.getUsers() }
        )
    }

    private fun searchUsers(query: String) {
        performNetworkCallWithCancellation<List<User>>(
            tag = "search",
            partialKey = "search",
            onSuccess = { updateState { copy(users = it, error = null) } },
            onError = { updateState { copy(error = it.message) } },
            networkCall = { api.searchUsers(query) }
        )
    }

    override fun UsersState.setGlobalLoadingState() = copy(isLoading = true)
    override fun UsersState.resetGlobalLoadingState() = copy(isLoading = false)
    override fun UsersState.setPartialLoadingState(key: String) =
        copy(loadingKeys = loadingKeys + key)
    override fun UsersState.resetPartialLoadingState(key: String) =
        copy(loadingKeys = loadingKeys - key)
}
```

## Typed Error Handling

All `onError` callbacks receive an `AppError` instead of a raw `String`, enabling precise error handling:

```kotlin
onError = { error ->
    when (error) {
        is AppError.Network -> {
            if (error.isRetryable) {
                updateState { copy(error = "Network issue, tap to retry") }
            } else {
                updateState { copy(error = "Server error: ${error.httpCode}") }
            }
        }
        is AppError.Timeout -> {
            updateState { copy(error = "Request timed out after ${error.durationMs}ms") }
        }
        else -> {
            updateState { copy(error = error.message) }
        }
    }
}
```

> **Tip:** For simple cases, just use `error.message` — all `AppError` subtypes provide a human-readable message.

