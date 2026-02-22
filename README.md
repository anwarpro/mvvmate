# MVVMate

A minimal, type-safe state management library for **Compose Multiplatform**, built on the MVI (Model-View-Intent) pattern.

[![Maven Central](https://img.shields.io/maven-central/v/com.helloanwar.mvvmate/core)](https://central.sonatype.com/artifact/com.helloanwar.mvvmate/core)
[![API Docs](https://img.shields.io/badge/docs-API-blue)](https://anwarpro.github.io/mvvmate)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Why MVVMate?

- **Zero boilerplate** — define State + Action + ViewModel and you're done
- **Type-safe** — compiler-enforced contracts between UI and business logic
- **Multiplatform** — Android, iOS, Desktop, and Web (WasmJS) from one codebase
- **Modular** — pick only what you need: core, network, actions, or all combined
- **Lifecycle-aware** — built on `androidx.lifecycle.ViewModel` with proper coroutine scoping

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| **core** | `com.helloanwar.mvvmate:core` | State management, actions, side effects |
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
            onError = { updateState { copy(error = it) } },
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
| [Network Guide](docs/network-guide.md) | Retry, timeout, cancellation, loading state management |
| [Actions Guide](docs/actions-guide.md) | Serial, parallel, chained, batch action dispatching |
| [Best Practices](docs/best-practices.md) | Architecture, state design, testing, Compose integration |

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
