# Core Module Guide

The `core` module is the foundation of MVVMate. It provides the base classes for state management, action handling, and side effects using the **MVI (Model-View-Intent)** pattern.

## Module Overview

| Class | Purpose |
|-------|---------|
| `BaseViewModel<S, A>` | Manages state and handles actions |
| `BaseViewModelWithEffect<S, A, E>` | Adds one-time side effect support (guaranteed delivery) |
| `AppError` | Typed error hierarchy for structured error handling |
| `MvvMateLogger` | Pluggable logging interface |
| `MvvMate` | Global configuration (logger, debug mode) |
| `UiState` | Marker interface for state classes |
| `UiAction` | Marker interface for user actions |
| `UiEffect` | Marker interface for side effects |

## Defining Contracts

Every screen in your app needs three things: **State**, **Action**, and optionally **Effect**.

```kotlin
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState

// 1. Define the state — everything the UI needs to render
data class ProfileState(
    val isLoading: Boolean = false,
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val error: String? = null
) : UiState

// 2. Define actions — everything the user can do
sealed interface ProfileAction : UiAction {
    data object LoadProfile : ProfileAction
    data class UpdateUsername(val name: String) : ProfileAction
    data class UpdateEmail(val email: String) : ProfileAction
    data object SaveProfile : ProfileAction
    data object Logout : ProfileAction
}

// 3. Define effects — one-time events (optional)
sealed interface ProfileEffect : UiEffect {
    data class ShowToast(val message: String) : ProfileEffect
    data object NavigateToLogin : ProfileEffect
    data class ShowError(val error: String) : ProfileEffect
}
```

> **Tip:** Use `sealed interface` instead of `sealed class` for actions and effects — it's more concise and allows multiple inheritance.

## BaseViewModel

### Creating a ViewModel

```kotlin
import com.helloanwar.mvvmate.core.BaseViewModel

class ProfileViewModel : BaseViewModel<ProfileState, ProfileAction>(
    initialState = ProfileState()
) {
    override suspend fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.LoadProfile -> {
                updateState { copy(isLoading = true) }
                // Load data from repository...
                updateState {
                    copy(
                        isLoading = false,
                        username = "John Doe",
                        email = "john@example.com"
                    )
                }
            }
            is ProfileAction.UpdateUsername -> {
                updateState { copy(username = action.name) }
            }
            is ProfileAction.UpdateEmail -> {
                updateState { copy(email = action.email) }
            }
            ProfileAction.SaveProfile -> {
                updateState { copy(isLoading = true) }
                // Save to API...
                updateState { copy(isLoading = false) }
            }
            ProfileAction.Logout -> {
                // Handle logout
            }
        }
    }
}
```

### Key Methods

| Method | Description |
|--------|-------------|
| `updateState { ... }` | Thread-safe state update via reducer lambda |
| `onAction(action)` | Override to handle each action type |
| `onError(action, error)` | Override to handle exceptions thrown in `onAction` |
| `state: StateFlow<S>` | Observable state for the UI |

### Using in Compose

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        CircularProgressIndicator()
    }

    TextField(
        value = state.username,
        onValueChange = { viewModel.handleAction(ProfileAction.UpdateUsername(it)) }
    )

    Button(onClick = { viewModel.handleAction(ProfileAction.SaveProfile) }) {
        Text("Save")
    }
}
```

## BaseViewModelWithEffect

Use this when you need **one-time events** that shouldn't be part of the permanent state — navigation, toasts, dialogs, etc.

> **Guaranteed delivery:** Effects are backed by a buffered `Channel`, so they are never lost even if no collector is active when the effect is emitted. They queue up and are delivered when a collector subscribes.

### Creating a ViewModel with Effects

```kotlin
import com.helloanwar.mvvmate.core.BaseViewModelWithEffect

class ProfileViewModel : BaseViewModelWithEffect<ProfileState, ProfileAction, ProfileEffect>(
    initialState = ProfileState()
) {
    override suspend fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.SaveProfile -> {
                updateState { copy(isLoading = true) }
                try {
                    // Save to API...
                    updateState { copy(isLoading = false) }
                    emitSideEffect(ProfileEffect.ShowToast("Profile saved!"))
                } catch (e: Exception) {
                    updateState { copy(isLoading = false) }
                    emitSideEffect(ProfileEffect.ShowError(e.message ?: "Save failed"))
                }
            }
            ProfileAction.Logout -> {
                // Clear session...
                emitSideEffect(ProfileEffect.NavigateToLogin)
            }
            // ... other actions
        }
    }
}
```

### Collecting Effects in Compose

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Collect one-time effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowToast -> {
                    // Show snackbar or toast
                }
                ProfileEffect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
                is ProfileEffect.ShowError -> {
                    // Show error dialog
                }
            }
        }
    }

    // ... render UI from state
}
```

## Error Handling

`BaseViewModel` includes a built-in error hook. If `onAction` throws an exception, `onError` is called automatically:

```kotlin
class SafeViewModel : BaseViewModel<MyState, MyAction>(MyState()) {

    override suspend fun onAction(action: MyAction) {
        when (action) {
            MyAction.FetchData -> {
                val data = riskyApiCall() // May throw!
                updateState { copy(data = data) }
            }
        }
    }

    override fun onError(action: MyAction, error: Exception) {
        // This is called automatically if onAction throws
        updateState { copy(error = error.message) }
    }
}
```

> **Note:** `CancellationException` is **never** caught by `onError` — it propagates normally to preserve structured concurrency.

## State Update Rules

1. **Always use `updateState`** — never mutate `_state` directly
2. **Reducers are atomic** — each `updateState` call is thread-safe
3. **Use `copy()`** — state classes should be `data class` for immutability
4. **Keep state flat** — avoid deeply nested objects for easier updates

```kotlin
// ✅ Good: flat state
data class MyState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val selectedId: String? = null
) : UiState

// ❌ Bad: deeply nested state
data class MyState(
    val ui: UiProperties = UiProperties(),
    val data: DataContainer = DataContainer()
) : UiState
```

## AppError — Typed Error Model

Instead of passing raw strings for errors, MVVMate provides a structured `AppError` sealed class:

```kotlin
import com.helloanwar.mvvmate.core.AppError

// Available error types:
AppError.Network(message, httpCode?, isRetryable)  // HTTP/connectivity errors
AppError.Timeout(message, durationMs)                // Timeout errors
AppError.Validation(message, field?)                 // Input validation errors  
AppError.Unknown(message, cause?)                    // Catch-all

// All types have a .message property for display:
updateState { copy(error = appError.message) }

// Pattern matching for specific handling:
when (error) {
    is AppError.Network -> if (error.isRetryable) retryLastAction()
    is AppError.Timeout -> showRetryDialog()
    is AppError.Validation -> highlightField(error.field)
    is AppError.Unknown -> logAndShowGeneric(error)
}

// Auto-classify exceptions:
val error = AppError.from(exception) // TimeoutException → Timeout, others → Unknown
```

## MvvMateLogger — Pluggable Logging

MVVMate automatically logs actions, state changes, effects, errors, and network lifecycle events through a pluggable logger:

```kotlin
import com.helloanwar.mvvmate.core.MvvMate
import com.helloanwar.mvvmate.core.PrintLogger

// Enable during development:
MvvMate.logger = PrintLogger
MvvMate.isDebug = true  // Enables state change logging
```

**What gets logged automatically:**

| Event | When | Debug-only? |
|-------|------|-------------|
| Action dispatch | Every `handleAction()` call | No |
| State change | Every `updateState()` with changed state | Yes |
| Effect emission | Every `emitSideEffect()` | No |
| Errors | Every exception in `onAction` | No |
| Network lifecycle | Start, retry, success, failure, cancel, timeout | No |

### Custom Logger

```kotlin
object TimberLogger : MvvMateLogger {
    override fun logAction(viewModelName: String, action: UiAction) {
        Timber.d("$viewModelName :: ${action::class.simpleName}")
    }
    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {
        Timber.v("$viewModelName :: $newState")
    }
    override fun logEffect(viewModelName: String, effect: Any) {
        Timber.d("$viewModelName :: $effect")
    }
    override fun logError(viewModelName: String, error: Throwable, context: String) {
        Timber.e(error, "$viewModelName [$context]")
    }
    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {
        Timber.d("Network [$tag] ${phase.name}: $details")
    }
}

// Set in Application.onCreate():
MvvMate.logger = TimberLogger
```

### AI-Powered App Logger

If you want an advanced logging mechanism that tracks the last `N` events in memory for crash reporting, use the `MvvMateAiLogger`. It includes an automatic `PrivacyRedactor` to strip sensitive Personal Identifiable Information (PII) before saving the crash dump.

```kotlin
val aiLogger = MvvMateAiLogger(
    delegate = TimberLogger, // Wrap your existing logger
    maxHistorySize = 50,     // Keep last 50 events in memory
    redactor = RegexPrivacyRedactor() // Strip emails, passwords, and CCs automatically
)

MvvMate.logger = aiLogger

// Setup a global UncaughtExceptionHandler (e.g., in Android Application class)
Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
    // Grab the grammatical timeline of exactly what happened before the crash
    val crashTimeline = aiLogger.takeRedactedSnapshotString()
    
    // Send this rich context to Crashlytics, Sentry, or your LLM backend
    Crashlytics.log(crashTimeline)
}
```

## AI Autopilot Bridge (Agentic UI)

MVVMate makes it trivial to turn any screen into an "Agentic UI", meaning you can allow an LLM or remote AI Agent to "drive" the application by dispatching standard `UiAction` events instead of relying on real human taps.

To do this safely without allowing an LLM to accidentally drop databases or perform unauthorized payments, you use the `AiActionBridge` and an `AiActionPolicy`.

### 1. Create a Security Policy
The Policy enforces what actions the LLM is allowed to execute based on the current state.

```kotlin
import com.helloanwar.mvvmate.core.ai.*

val aiPolicy = object : AiActionPolicy<CartState, CartAction> {
    override fun isActionAllowed(action: CartAction, currentState: CartState): Boolean {
        // AI is allowed to Add/Remove items
        if (action is CartAction.AddItem || action is CartAction.RemoveItem) return true
        
        // AI is strictly FORBIDDEN from clearing the cart or checking out
        if (action is CartAction.ClearCart || action is CartAction.Checkout) return false
        
        return false // Default deny
    }
}
```

### 2. Connect the Bridge
Bind the LLM bridge to your ViewModel. You must provide a parser that converts the raw text the LLM outputs (like JSON) back into your sealed action class.

```kotlin
val bridge = AiActionBridge(
    viewModel = cartViewModel,
    policy = aiPolicy,
    parser = MyJsonActionParser(), // Your custom deserializer
    redactor = RegexPrivacyRedactor() // Strip sensitive data before giving state to AI
)
```

### 3. Execution Loop
Read the safe, redacted state to feed to your LLM prompt, and pass the resulting string backward.

```kotlin
// Safely feed standard JSON state to your LLM without leaking passwords
val statePromptContext = bridge.getCurrentState() 
val llmResponseString = callLlm(statePromptContext) // e.g. "{ "type": "AddItem" }"

// Dispatch safely
val result = bridge.dispatch(llmResponseString)
if (result.isFailure) {
    // LLM tried to hallucinate a bad command or bypass security policy
}
```

