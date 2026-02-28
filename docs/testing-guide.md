# Testing Module Guide

The `testing` module provides a Turbine-based DSL for testing MVVMate ViewModels. It gives you exact, chronological assertions over state emissions and side effects.

## Module Overview

| Class | Purpose |
|-------|---------|
| `ViewModelTestScope<S, A>` | DSL scope for testing `BaseViewModel` state sequences |
| `ViewModelEffectTestScope<S, A, E>` | DSL scope for testing interleaved states and effects |
| `ViewModelEvent<S, E>` | Sealed class multiplexing `State` and `Effect` emissions |

## Installation

```kotlin
commonTest.dependencies {
    implementation("com.helloanwar.mvvmate:testing:<version>")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<version>")
}
```

> The `testing` module depends on [Turbine](https://github.com/cashapp/turbine) and the `core` module.

## Testing State-Only ViewModels

Use `viewModel.test { }` for ViewModels that extend `BaseViewModel` (no side effects):

```kotlin
@Test
fun testCounterIncrement() = runTest {
    val viewModel = CounterViewModel()

    viewModel.test {
        // Initial state is automatically emitted
        expectStateEquals(CounterState(count = 0))

        // Dispatch action and assert resulting state
        dispatchAction(CounterAction.Increment)
        expectStateEquals(CounterState(count = 1))

        // Use lambda assertions for partial checks
        dispatchAction(CounterAction.Increment)
        expectState { it.count == 2 }
    }
}
```

### ViewModelTestScope API

| Method | Description |
|--------|-------------|
| `dispatchAction(action)` | Dispatch a `UiAction` to the ViewModel under test |
| `expectState { predicate }` | Await next state and assert the predicate returns `true` |
| `expectStateEquals(state)` | Await next state and assert strict equality |
| `awaitState()` | Await and return the next emitted state without assertions |
| `skipStates(count)` | Consume and ignore the next `count` state emissions |
| `cancelAndIgnoreRemainingStates()` | Cancel collection, discard remaining events |
| `rawTurbine` | Access the underlying Turbine `ReceiveTurbine` for advanced operations |

### Parameters

Both `test` and `testEffects` accept optional parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `timeout` | `Duration?` | Custom timeout for the test (default: Turbine's default) |
| `name` | `String?` | Name for test output/debugging |

## Testing ViewModels with Side Effects

Use `viewModel.testEffects { }` for ViewModels that extend `BaseViewModelWithEffect`. This merges state and effect emissions into a single chronological stream:

```kotlin
@Test
fun testLoginFlow() = runTest {
    val viewModel = LoginViewModel(FakeAuthRepository(shouldSucceed = true))

    viewModel.testEffects {
        // Initial state
        expectState { !it.isLoading }

        // Start login
        dispatchAction(LoginAction.Submit("user@test.com", "password"))

        // Assert loading state
        expectState { it.isLoading }

        // Assert the side effect
        expectEffectEquals(LoginEffect.NavigateToHome)

        // Assert final state
        expectState { !it.isLoading }
    }
}
```

### ViewModelEffectTestScope API

| Method | Description |
|--------|-------------|
| `dispatchAction(action)` | Dispatch a `UiAction` to the ViewModel under test |
| `expectState { predicate }` | Await next emission, assert it is a **State** matching predicate |
| `expectStateEquals(state)` | Await next emission, assert it is a **State** equal to expected |
| `expectEffectEquals(effect)` | Await next emission, assert it is an **Effect** equal to expected |
| `expectEffectClass<T>()` | Await next emission, assert it is an **Effect** of type `T` |
| `awaitEvent()` | Await and return the next emission (State or Effect) |
| `skipEvents(count)` | Consume and ignore the next `count` emissions |
| `cancelAndIgnoreRemainingEvents()` | Cancel collection, discard remaining events |
| `rawTurbine` | Access the underlying Turbine `ReceiveTurbine` for advanced operations |

> **Type Safety:** If you call `expectState` but the next emission is an Effect (or vice-versa), the test fails with a descriptive error message.

## Testing Patterns

### Testing Multiple Actions

```kotlin
@Test
fun testMultipleActions() = runTest {
    val viewModel = TodoViewModel()

    viewModel.test {
        awaitState() // skip initial

        dispatchAction(TodoAction.Add("Buy milk"))
        expectState { it.items.size == 1 }

        dispatchAction(TodoAction.Add("Walk dog"))
        expectState { it.items.size == 2 }

        dispatchAction(TodoAction.Remove("Buy milk"))
        expectState { it.items.size == 1 && it.items.first().text == "Walk dog" }
    }
}
```

### Testing Error Handling

```kotlin
@Test
fun testErrorHandling() = runTest {
    val repo = FakeUserRepository(shouldFail = true)
    val viewModel = UsersViewModel(repo)

    viewModel.test {
        awaitState() // skip initial

        dispatchAction(UsersAction.FetchUsers)
        expectState { it.isLoading }
        expectState { it.error != null && !it.isLoading }
    }
}
```

### Testing Specific Effect Types

```kotlin
@Test
fun testToastEffect() = runTest {
    val viewModel = CartViewModel()

    viewModel.testEffects {
        awaitEvent() // skip initial state

        dispatchAction(CartAction.AddItem(testProduct))
        skipEvents(1) // skip the state update

        // Assert exact effect type without caring about the value
        val toast = expectEffectClass<CartEffect.ShowToast>()
        assertEquals("Item added!", toast.message)
    }
}
```

### Skipping Intermediate States

When an action causes multiple state updates, use `skipStates` or `skipEvents` to jump to the one you care about:

```kotlin
@Test
fun testSkipIntermediateStates() = runTest {
    val viewModel = SetupViewModel()

    viewModel.test {
        awaitState() // initial

        dispatchAction(SetupAction.RunFullSetup)
        skipStates(3) // skip loading states
        expectState { it.setupComplete }
    }
}
```

## Setup Tips

### Dispatcher Management

Always use `runTest` from `kotlinx-coroutines-test`. For ViewModels that use `viewModelScope`, set the main dispatcher:

```kotlin
class MyViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun myTest() = runTest {
        // ...
    }
}
```

### Use Fakes

Prefer fakes over mocking libraries for cleaner, more maintainable tests:

```kotlin
class FakeAuthRepository : AuthRepository {
    var shouldSucceed = true

    override suspend fun login(email: String, password: String): Boolean {
        return shouldSucceed
    }
}
```
