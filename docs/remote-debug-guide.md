# Remote Debug Module Guide

The `remote-debug` module connects your running app to the MVVMate Studio Plugin via WebSocket, enabling **live event streaming** and **time-travel debugging** directly from your IDE.

## Module Overview

| Class | Purpose |
|-------|---------|
| `RemoteDebugLogger` | WebSocket-based `MvvMateLogger` that streams events to the IDE |
| `DebugPayload` | Data sent from the app to the IDE (actions, state changes, effects, errors, network) |
| `DebugCommand` | Commands sent from the IDE to the app (time-travel, action injection) |
| `CommandType` | `SET_STATE` (time-travel) or `INJECT_ACTION` (inject actions from IDE) |
| `PayloadType` | `ACTION`, `STATE_CHANGE`, `EFFECT`, `ERROR`, `NETWORK` |

## Installation

```kotlin
commonMain.dependencies {
    implementation("com.helloanwar.mvvmate:remote-debug:<version>")
}
```

> The `remote-debug` module depends on `core`, Ktor Client WebSockets, and `kotlinx-serialization`.

## Setup

### 1. Create and Attach the Logger

```kotlin
// In your Application.onCreate() or main():
val debugLogger = RemoteDebugLogger(
    host = "127.0.0.1",   // IDE host (default)
    port = 8080,           // WebSocket port (default)
    path = "/ws/mvvmate"   // WebSocket path (default)
)

MvvMate.logger = debugLogger
MvvMate.isDebug = true  // Required for state change logging and debug bridge
```

### 2. Enable Action Injection (Optional)

To allow the IDE to inject actions into your ViewModels, override `mapDebugAction` in each ViewModel:

```kotlin
class CartViewModel : BaseViewModel<CartState, CartAction>(CartState()) {

    override fun mapDebugAction(payload: String): CartAction? {
        return when (payload) {
            "AddItem" -> CartAction.AddItem(Product.sample())
            "RemoveItem" -> CartAction.RemoveItem("sample-id")
            "ClearCart" -> CartAction.ClearCart
            "Checkout" -> CartAction.Checkout
            else -> null
        }
    }

    override suspend fun onAction(action: CartAction) {
        // ... normal action handling
    }
}
```

### 3. Disconnect on Cleanup

```kotlin
// When the app is shutting down or leaving debug mode:
debugLogger.disconnect()
```

## How It Works

### App → IDE: Event Streaming

The `RemoteDebugLogger` implements `MvvMateLogger` and sends every logged event as a JSON `DebugPayload` over WebSocket:

```
App                                    IDE (Studio Plugin)
 │                                          │
 │──── DebugPayload (ACTION) ──────────────▶│
 │──── DebugPayload (STATE_CHANGE) ────────▶│
 │──── DebugPayload (EFFECT) ──────────────▶│
 │──── DebugPayload (ERROR) ───────────────▶│
 │──── DebugPayload (NETWORK) ─────────────▶│
```

Each payload includes:

| Field | Description |
|-------|-------------|
| `type` | `ACTION`, `STATE_CHANGE`, `EFFECT`, `ERROR`, or `NETWORK` |
| `viewModelName` | The ViewModel's `logTag` |
| `payload` | The primary data (action name, state string, error message, etc.) |
| `additionalData` | Secondary data (old state for diffs, error context, network phase) |
| `timestamp` | Epoch milliseconds |

### IDE → App: Commands

The IDE can send `DebugCommand` messages back to the app:

```
IDE (Studio Plugin)                    App
 │                                      │
 │──── DebugCommand (SET_STATE) ───────▶│  → Restores a previous state snapshot
 │──── DebugCommand (INJECT_ACTION) ───▶│  → Calls mapDebugAction + handleAction
```

## Time-Travel Debugging

The `RemoteDebugLogger` automatically stores up to **500 state snapshots** in memory. When the IDE sends a `SET_STATE` command with a snapshot index, the app restores that exact state to the ViewModel.

This works through the `MvvMate.debugBridge` registry — ViewModels auto-register themselves when `MvvMate.isDebug = true` and they receive their first action.

```
User taps "Increment" 5 times → 5 state snapshots stored
IDE sends SET_STATE with index=2 → App restores state from snapshot #2
UI immediately reflects the restored state
```

> **Requirement:** `MvvMate.isDebug` must be `true` for time-travel to work. ViewModels register their `DebugBridge` on first action dispatch.

## Action Injection

The IDE can dispatch actions to any registered ViewModel:

1. IDE sends `DebugCommand(type = INJECT_ACTION, viewModelName = "CartViewModel", payload = "AddItem")`
2. `RemoteDebugLogger` looks up the ViewModel in `MvvMate.debugBridge`
3. Calls `bridge.injectAction("AddItem")` → which calls `mapDebugAction("AddItem")`
4. If the action is resolved, it's dispatched via `handleAction()`

You can also listen for commands with the `onCommandReceived` callback:

```kotlin
debugLogger.onCommandReceived = { command ->
    println("IDE sent command: ${command.type} → ${command.viewModelName}: ${command.payload}")
}
```

## Protocol Reference

### DebugPayload (App → IDE)

```json
{
  "type": "STATE_CHANGE",
  "viewModelName": "CartViewModel",
  "payload": "CartState(items=[Item(id=1)], total=29.99)",
  "additionalData": "CartState(items=[], total=0.0)",
  "timestamp": 1709123456789
}
```

### DebugCommand (IDE → App)

```json
{
  "type": "SET_STATE",
  "viewModelName": "CartViewModel",
  "payload": "3"
}
```

```json
{
  "type": "INJECT_ACTION",
  "viewModelName": "CartViewModel",
  "payload": "AddItem"
}
```

## Connection Behavior

- The logger connects on initialization and queues messages via an `UNLIMITED` channel
- If the WebSocket connection fails, messages are consumed and discarded to prevent memory leaks
- The logger runs on `Dispatchers.Default` with a `SupervisorJob`
- Calling `disconnect()` closes the session, HTTP client, and payload channel

## Production Safety

> **Warning:** Never ship `remote-debug` in production builds. Guard it behind a debug flag:

```kotlin
if (BuildConfig.DEBUG) {
    val debugLogger = RemoteDebugLogger()
    MvvMate.logger = debugLogger
    MvvMate.isDebug = true
}
```

Or use Gradle to include the dependency only in debug builds:

```kotlin
commonMain.dependencies {
    // Only in debug
    if (project.findProperty("enableDebug") == "true") {
        implementation("com.helloanwar.mvvmate:remote-debug:<version>")
    }
}
```
