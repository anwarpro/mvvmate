# MVVMate

A minimal, type-safe state management library for **Compose Multiplatform**, built on the MVI (Model-View-Intent) pattern.

[![Maven Central](https://img.shields.io/maven-central/v/com.helloanwar.mvvmate/core)](https://central.sonatype.com/artifact/com.helloanwar.mvvmate/core)
[![API Docs](https://img.shields.io/badge/docs-API-blue)](https://anwarpro.github.io/mvvmate)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Why MVVMate?

- **Zero boilerplate** — define State + Action + ViewModel and you're done
- **Type-safe** — compiler-enforced contracts with typed error model (`AppError`)
- **Multiplatform** — Android, iOS, Desktop, and Web (WasmJS) from one codebase
- **Modular** — pick only what you need: core, network, actions, or all combined
- **Lifecycle-aware** — built on `androidx.lifecycle.ViewModel` with proper coroutine scoping
- **Observable** — pluggable logging for actions, state, effects, and network lifecycle

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| **core** | `com.helloanwar.mvvmate:core` | State management, actions, side effects |
| **testing** | `com.helloanwar.mvvmate:testing` | Flow testing DSL for ViewModels with `turbine` |
| **forms** | `com.helloanwar.mvvmate:forms` | Declarative, type-safe form validation for UiState |
| **network** | `com.helloanwar.mvvmate:network` | Network calls with retry, timeout, cancellation |
| **actions** | `com.helloanwar.mvvmate:actions` | Serial, parallel, chained, batch action dispatching |
| **network-actions** | `com.helloanwar.mvvmate:network-actions` | Combined network + actions capabilities |

## Platform Support

| Platform | Status |
|----------|--------|
| Android | ✅ |
| iOS (arm64, x64, simulatorArm64) | ✅ |
| Desktop (JVM) | ✅ |
| Web (WasmJS) | ✅ |

## Installation

Add the modules you need to your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core (required)
            implementation("com.helloanwar.mvvmate:core:<version>")

            // Optional modules — pick what you need
            implementation("com.helloanwar.mvvmate:network:<version>")
            implementation("com.helloanwar.mvvmate:actions:<version>")
            implementation("com.helloanwar.mvvmate:network-actions:<version>")
        }
    }
}
```

Check [Maven Central](https://central.sonatype.com/artifact/com.helloanwar.mvvmate/core) for the latest version.

## Quick Start

### 1. Define your contracts

```kotlin
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : UiState

sealed interface CounterAction : UiAction {
    data object Increment : CounterAction
    data object Decrement : CounterAction
    data object Reset : CounterAction
}
```

### 2. Create your ViewModel

```kotlin
class CounterViewModel : BaseViewModel<CounterState, CounterAction>(
    initialState = CounterState()
) {
    override suspend fun onAction(action: CounterAction) {
        when (action) {
            CounterAction.Increment -> updateState { copy(count = count + 1) }
            CounterAction.Decrement -> updateState { copy(count = count - 1) }
            CounterAction.Reset -> updateState { copy(count = 0) }
        }
    }
}
```

### 3. Connect to your Composable

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Count: ${state.count}", style = MaterialTheme.typography.headlineLarge)

        Row {
            Button(onClick = { viewModel.handleAction(CounterAction.Decrement) }) {
                Text("-")
            }
            Button(onClick = { viewModel.handleAction(CounterAction.Increment) }) {
                Text("+")
            }
        }

        TextButton(onClick = { viewModel.handleAction(CounterAction.Reset) }) {
            Text("Reset")
        }
    }
}
```

## Architecture Diagram

```
┌─────────────────────────────────────────────┐
│                 Composable UI               │
│  ┌──────────┐         ┌──────────────────┐  │
│  │  state   │◄────────│ collectAsState() │  │
│  └──────────┘         └──────────────────┘  │
│       ▲                       │              │
│       │                handleAction()        │
│       │                       ▼              │
│  ┌────┴─────────────────────────────┐       │
│  │           ViewModel              │       │
│  │  ┌─────────┐   ┌─────────────┐  │       │
│  │  │  State  │   │  onAction() │  │       │
│  │  │  Flow   │◄──│  (reducer)  │  │       │
│  │  └─────────┘   └─────────────┘  │       │
│  │  ┌──────────────────────────┐   │       │
│  │  │   Side Effects (opt.)    │───┼───────│
│  │  └──────────────────────────┘   │       │
│  └──────────────────────────────────┘       │
└─────────────────────────────────────────────┘
```

## Side Effects Example

For one-time events like navigation, toasts, or dialogs:

```kotlin
class LoginViewModel : BaseViewModelWithEffect<LoginState, LoginAction, LoginEffect>(
    initialState = LoginState()
) {
    override suspend fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.Submit -> {
                updateState { copy(isLoading = true) }
                val success = authRepo.login(action.email, action.password)
                updateState { copy(isLoading = false) }
                if (success) {
                    emitSideEffect(LoginEffect.NavigateToHome)
                } else {
                    emitSideEffect(LoginEffect.ShowError("Invalid credentials"))
                }
            }
        }
    }
}
```

## Testing

MVVMate provides a robust flow-testing DSL based on CashApp's Turbine framework using the `testing` artifact.

Add the optional dependency:
```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation("com.helloanwar.mvvmate:testing:<version>")
        }
    }
}
```

### Standard ViewModel Testing
For a simple `BaseViewModel` containing only states and actions:

```kotlin
@Test
fun testCounterViewModel() = runTest {
    val viewModel = CounterViewModel()

    viewModel.test {
        // Automatically skips the initial emitted state or you can assert it:
        expectStateEquals(CounterState(count = 0))

        dispatchAction(CounterAction.Increment)
        expectStateEquals(CounterState(count = 1))

        dispatchAction(CounterAction.Decrement)
        expectState { it.count == 0 } // Assert lambda
    }
}
```

### Side Effect Testing
For a `BaseViewModelWithEffect`, you can assert `State` and `Effect` emissions in chronological order:

```kotlin
@Test
fun testLoginViewModel() = runTest {
    val viewModel = LoginViewModel()

    viewModel.testEffects {
        // Assert initial state
        expectState { !it.isLoading }

        // Start operation
        dispatchAction(LoginAction.Submit("test@test.com", "pass"))
        expectState { it.isLoading }

        // Wait for the simulated effect, assert exact type
        val effect = expectEffectClass<LoginEffect.NavigateToHome>()

        // Assert ending state
        expectState { !it.isLoading }
    }
}
```

## Form Validation

MVVMate provides a declarative, type-safe validation system through the `forms` module.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.helloanwar.mvvmate:forms:<version>")
        }
    }
}
```

### 1. Define Form State
Use `FormField<T>` inside your `UiState`:

```kotlin
data class RegistrationState(
    val email: FormField<String> = FormField(""),
    val age: FormField<String> = FormField(""),
    val isSubmitting: Boolean = false
) : UiState {
    val isFormValid: Boolean get() = email.isValid && age.isValid
}
```

### 2. Update and Validate
Use `setValue` and built-in validators to cleanly update state and run validation rules inline:

```kotlin
class RegistrationViewModel : BaseViewModel<RegistrationState, RegistrationAction>(RegistrationState()) {
    override suspend fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.EmailChanged -> updateState {
                copy(email = email.setValue(
                    newValue = action.email, 
                    Validators.required(), 
                    Validators.email()
                ))
            }
            is RegistrationAction.AgeChanged -> updateState {
                copy(age = age.setValue(
                    newValue = action.age,
                    Validators.required(),
                    Validators.digitsRequired("Must be a valid number")
                ))
            }
            RegistrationAction.Submit -> {
                if (state.value.isFormValid) {
                    // Proceed with submission
                } else {
                    // Mark all fields as touched to show errors
                    updateState {
                        copy(
                            email = email.markTouched(Validators.required(), Validators.email()),
                            age = age.markTouched(Validators.required(), Validators.digitsRequired("Must be a valid number"))
                        )
                    }
                }
            }
        }
    }
}
```

## Network Call Example

```kotlin
class ProductsViewModel : BaseNetworkViewModel<ProductsState, ProductsAction>(
    initialState = ProductsState()
) {
    override suspend fun onAction(action: ProductsAction) {
        when (action) {
            ProductsAction.Load -> loadProducts()
        }
    }

    private suspend fun loadProducts() {
        performNetworkCallWithRetry<List<Product>>(
            retries = 3,
            isGlobal = true,
            onSuccess = { updateState { copy(products = it) } },
            onError = { error -> updateState { copy(error = error.message) } },
            networkCall = { api.getProducts() }
        )
    }

    override fun ProductsState.setGlobalLoadingState() = copy(isLoading = true)
    override fun ProductsState.resetGlobalLoadingState() = copy(isLoading = false)
}
```

## Detailed Guides

| Guide | Description |
|-------|-------------|
| [Core Guide](docs/core-guide.md) | BaseViewModel, BaseViewModelWithEffect, contracts, error handling |
| [Network Guide](docs/network-guide.md) | Retry, timeout, cancellation, loading state management, typed errors |
| [Actions Guide](docs/actions-guide.md) | Serial, parallel, chained, batch action dispatching |
| [Best Practices](docs/best-practices.md) | Architecture, state design, testing, logging, Compose integration |

## Logger Setup

MVVMate includes a pluggable logging system. Enable it during development:

```kotlin
// In your Application.onCreate() or main():
MvvMate.logger = PrintLogger  // Built-in console logger
MvvMate.isDebug = true         // Enable state change logging
```

### AI Crash Logger

You can use the built-in `MvvMateAiLogger` to maintain a secure, GDPR-compliant ring buffer of chronological actions, states, networking, and side effects. If a crash occurs, you instantly get a perfect, human-readable timeline to feed into an LLM or logging service:

```kotlin
val aiLogger = MvvMateAiLogger(
    delegate = PrintLogger, // also print to console
    maxHistorySize = 50,
    // Safely redacts emails, tokens, and credit cards from the final string output
    redactor = RegexPrivacyRedactor(RegexPrivacyRedactor.DefaultPatterns())
)
MvvMate.logger = aiLogger

// When a crash occurs:
val crashContextString = aiLogger.takeRedactedSnapshotString()
```

### LLM Autopilot Bridge (Agentic UI)

Want to let an AI "drive" your app? The `AiActionBridge` connects an LLM directly to your `BaseViewModel`. 

It includes a strict `AiActionPolicy` to ensure the LLM can only execute safe, whitelisted actions, preventing it from doing things like deleting accounts or triggering payments.

```kotlin
// 1. Define a security policy
val safePolicy = object : AiActionPolicy<MyState, MyAction> {
    override fun isActionAllowed(action: MyAction, currentState: MyState): Boolean {
        // AI is strictly FORBIDDEN from deleting accounts or checking out
        return action !is MyAction.DeleteAccount && action !is MyAction.Checkout
    }
}

// 2. Attach Bridge
val bridge = AiActionBridge(
    viewModel = myViewModel,
    policy = safePolicy,
    parser = MyJsonActionParser() // Convert LLM strings to UiAction
)

// 3. Receive LLM Command
// Example AI generated JSON: { "type": "Increment" }
bridge.dispatch(llmJsonOutput)
```

Or implement `MvvMateLogger` interface for custom integrations (Timber, Napier, etc.).

See [Best Practices → Logging](docs/best-practices.md#logging) for details.

## API Documentation

Full API docs generated by Dokka: [anwarpro.github.io/mvvmate](https://anwarpro.github.io/mvvmate)

## Contributing

Contributions are welcome! Please see the issues tab for areas where help is needed.

## License

```
MIT License

Copyright (c) 2024 Mohammad Anwar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```
