# Best Practices

Patterns, conventions, and guidelines for building maintainable Compose Multiplatform apps with MVVMate.

## Architecture

### 1. One ViewModel per Screen

Each screen should have its own ViewModel with dedicated State, Action, and Effect contracts:

```
feature/
├── ProfileContracts.kt     // State, Action, Effect
├── ProfileViewModel.kt     // Business logic
└── ProfileScreen.kt        // Composable UI
```

### 2. Keep ViewModels Pure

ViewModels should not know about UI details — no `Context`, no `NavController`, no Compose APIs:

```kotlin
// ✅ Good: ViewModel emits an effect, UI handles navigation
override suspend fun onAction(action: ProfileAction) {
    when (action) {
        ProfileAction.Logout -> {
            repository.clearSession()
            emitSideEffect(ProfileEffect.NavigateToLogin)
        }
    }
}

// ❌ Bad: ViewModel handles navigation directly
override suspend fun onAction(action: ProfileAction) {
    when (action) {
        ProfileAction.Logout -> {
            navController.navigate("login") // Don't do this!
        }
    }
}
```

### 3. Choose the Right Base Class

| Scenario | Base Class |
|----------|-----------|
| Simple state management | `BaseViewModel` |
| Need one-time events (toasts, navigation) | `BaseViewModelWithEffect` |
| Multiple coordinated operations | `BaseActionsViewModel` |
| Network calls with loading/retry/timeout | `BaseNetworkViewModel` |
| Network + coordinated operations | `BaseNetworkActionsViewModel` |

### 4. Inject Dependencies

Pass dependencies through constructors, not created internally:

```kotlin
// ✅ Good: injectable, testable
class UsersViewModel(
    private val userRepository: UserRepository,
    private val analytics: Analytics
) : BaseNetworkViewModel<UsersState, UsersAction>(UsersState()) {
    // ...
}

// ❌ Bad: hard-coded dependencies
class UsersViewModel : BaseNetworkViewModel<UsersState, UsersAction>(UsersState()) {
    private val api = ApiClient() // Not testable!
}
```

## State Design

### 5. Use Data Classes with Sensible Defaults

```kotlin
data class ProductState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val selectedCategory: Category? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
) : UiState
```

### 6. Derive Values, Don't Store Them

Computed properties reduce state inconsistency:

```kotlin
data class CartState(
    val items: List<CartItem> = emptyList(),
    val couponCode: String? = null,
    val couponDiscount: Double = 0.0
) : UiState {
    // Derived — always consistent with items
    val subtotal: Double get() = items.sumOf { it.price * it.quantity }
    val total: Double get() = (subtotal - couponDiscount).coerceAtLeast(0.0)
    val itemCount: Int get() = items.sumOf { it.quantity }
    val isEmpty: Boolean get() = items.isEmpty()
}
```

### 7. Use Sealed Types for Mutually Exclusive States

```kotlin
// ✅ Good: impossible to be loading AND have data
data class ScreenState(
    val content: ContentState = ContentState.Idle
) : UiState

sealed interface ContentState {
    data object Idle : ContentState
    data object Loading : ContentState
    data class Success(val data: List<Item>) : ContentState
    data class Error(val message: String) : ContentState
}

// ❌ Bad: can be loading=true with data and error simultaneously
data class ScreenState(
    val isLoading: Boolean = false,
    val data: List<Item>? = null,
    val error: String? = null
) : UiState
```

## Action Design

### 8. Use Sealed Interfaces

```kotlin
// ✅ Prefer sealed interface — concise, allows multi-inheritance
sealed interface CartAction : UiAction {
    data class AddItem(val product: Product) : CartAction
    data class RemoveItem(val itemId: String) : CartAction
    data class UpdateQuantity(val itemId: String, val qty: Int) : CartAction
    data object Checkout : CartAction
}

// Avoid sealed class for simple cases
```

### 9. Actions Should Represent User Intent, Not Implementation

```kotlin
// ✅ Good: describes what the user wants
sealed interface SearchAction : UiAction {
    data class Search(val query: String) : SearchAction
    data object ClearSearch : SearchAction
    data object LoadNextPage : SearchAction
}

// ❌ Bad: exposes implementation details to the UI
sealed interface SearchAction : UiAction {
    data class SetQueryAndCallApi(val query: String) : SearchAction
    data object ResetStateAndClearCache : SearchAction
}
```

## Error Handling

### 10. Always Override `onError` for Production Apps

```kotlin
class ProductionViewModel(
    private val crashReporter: CrashReporter
) : BaseViewModel<MyState, MyAction>(MyState()) {

    override suspend fun onAction(action: MyAction) {
        // Your normal logic — exceptions propagate to onError
    }

    override fun onError(action: MyAction, error: Exception) {
        // Log to crash reporter
        crashReporter.recordException(error, mapOf("action" to action.toString()))

        // Update UI with user-friendly message
        updateState { copy(error = "Something went wrong. Please try again.") }
    }
}
```

### 11. Handle Errors at the Right Level

```kotlin
override suspend fun onAction(action: OrderAction) {
    when (action) {
        OrderAction.PlaceOrder -> {
            // ✅ Handle expected errors locally where you have context
            performNetworkCall<Order>(
                isGlobal = true,
                onSuccess = { order ->
                    updateState { copy(orderId = order.id) }
                    emitSideEffect(OrderEffect.ShowConfirmation(order.id))
                },
                onError = { error ->
                    // Specific, contextual error handling
                    updateState { copy(error = "Order failed: $error") }
                },
                networkCall = { api.placeOrder(state.value.cart) }
            )
        }
    }
}

// Let unexpected errors bubble to onError for global handling
override fun onError(action: OrderAction, error: Exception) {
    crashReporter.log(error)
    updateState { copy(error = "Unexpected error occurred") }
}
```

## Coroutines

### 12. Don't Block the Main Thread

```kotlin
override suspend fun onAction(action: MyAction) {
    when (action) {
        MyAction.ProcessData -> {
            // ✅ Move heavy work off the main thread
            val result = withContext(Dispatchers.Default) {
                heavyComputation(data)
            }
            updateState { copy(result = result) }
        }
    }
}
```

### 13. Use Cancellation-Aware Code

```kotlin
private suspend fun longRunningTask() {
    val items = api.getAllItems()
    for (item in items) {
        // ✅ Check for cancellation in long loops
        ensureActive()
        processItem(item)
    }
}
```

## Testing

### 14. Test ViewModels with `runTest`

```kotlin
class CartViewModelTest {
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
    fun `adding item updates cart state`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository())

        viewModel.handleAction(CartAction.AddItem(testProduct))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(testProduct.id, viewModel.state.value.items.first().productId)
    }

    @Test
    fun `checkout emits navigation effect`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository())
        val effects = mutableListOf<CartEffect>()

        // Collect effects in background
        val job = launch { viewModel.sideEffects.collect { effects.add(it) } }

        viewModel.handleAction(CartAction.Checkout)
        advanceUntilIdle()

        assertTrue(effects.any { it is CartEffect.NavigateToPayment })
        job.cancel()
    }
}
```

### 15. Use Fakes, Not Mocks

```kotlin
// ✅ Simple fake
class FakeUserRepository : UserRepository {
    var users = mutableListOf<User>()
    var shouldFail = false

    override suspend fun getUsers(): List<User> {
        if (shouldFail) throw IOException("Network error")
        return users
    }
}
```

## Compose Integration

### 16. Collect State Properly

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    // ✅ Use collectAsState — recomposes only when state changes
    val state by viewModel.state.collectAsState()

    // ✅ Collect effects in LaunchedEffect
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { /* handle effect */ }
    }
}
```

### 17. Dispatch Actions from Callbacks

```kotlin
@Composable
fun SearchBar(onAction: (SearchAction) -> Unit) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = {
            text = it
            onAction(SearchAction.Search(it)) // Dispatch on change
        }
    )
}

// Usage:
SearchBar(onAction = viewModel::handleAction)
```
