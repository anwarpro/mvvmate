# Forms Module Guide

The `forms` module provides a declarative, type-safe validation system for managing form state entirely within your `UiState`. No external state holders, no side channels — just clean data classes with built-in validation.

## Module Overview

| Class | Purpose |
|-------|---------|
| `FormField<T>` | Holds a field's value, errors, and metadata (touched, dirty) |
| `Validator<T>` | Type alias `(T) -> String?` — returns error message or null |
| `Validators` | Collection of built-in validators (required, email, minLength, etc.) |

## Installation

```kotlin
commonMain.dependencies {
    implementation("com.helloanwar.mvvmate:forms:<version>")
}
```

## FormField\<T\>

A `FormField` wraps a single form value with validation metadata:

```kotlin
data class FormField<T>(
    val value: T,               // Current value
    val errors: List<String> = emptyList(), // Validation error messages
    val isTouched: Boolean = false,         // User has interacted
    val isDirty: Boolean = false            // Value has been changed
)
```

### Computed Properties

| Property | Type | Description |
|----------|------|-------------|
| `isValid` | `Boolean` | `true` if `errors` is empty |
| `isInvalid` | `Boolean` | `true` if `errors` is not empty |

### Key Methods

#### `setValue(newValue, vararg validators)`

Updates the value, runs all validators, and marks the field as dirty and touched:

```kotlin
val field = FormField("")
val updated = field.setValue("hello@test.com", Validators.required(), Validators.email())
// updated.value = "hello@test.com"
// updated.isDirty = true
// updated.isTouched = true
// updated.errors = [] (valid email)
```

#### `markTouched(vararg validators)`

Marks the field as touched **without changing its value**, and runs validators. Use this on form submit to surface errors on fields the user hasn't interacted with yet:

```kotlin
val field = FormField("") // untouched, no errors yet
val touched = field.markTouched(Validators.required())
// touched.isTouched = true
// touched.errors = ["Required"]
```

## Built-in Validators

All validators return `null` (valid) or an error message string (invalid):

| Validator | Description | Default Message |
|-----------|-------------|-----------------|
| `Validators.required(message)` | Not null or blank | `"Required"` |
| `Validators.email(message)` | Standard email format | `"Invalid email"` |
| `Validators.minLength(len, message)` | At least `len` characters | `"Must be at least {len} characters"` |
| `Validators.maxLength(len, message)` | At most `len` characters | `"Must be at most {len} characters"` |
| `Validators.pattern(regex, message)` | Matches regex | `"Invalid format"` |
| `Validators.digitsRequired(message)` | Only digits (0-9) | `"Must contain only digits"` |
| `Validators.decimalRequired(message)` | Valid decimal number | `"Must be a valid decimal number"` |

> **Note:** Validators for `String?` types skip validation if the value is `null` or blank (except `required`). This means chaining `required()` + `email()` won't double-report on an empty field.

## Custom Validators

Since `Validator<T>` is just a `(T) -> String?` typealias, you can create custom validators trivially:

```kotlin
fun passwordStrength(): Validator<String?> = { value ->
    when {
        value == null || value.length < 8 -> "Must be at least 8 characters"
        !value.any { it.isUpperCase() } -> "Must contain an uppercase letter"
        !value.any { it.isDigit() } -> "Must contain a digit"
        else -> null
    }
}

// Usage:
updateState {
    copy(password = password.setValue(action.value, Validators.required(), passwordStrength()))
}
```

## Complete Example

### 1. Define Form State

```kotlin
data class RegistrationState(
    val email: FormField<String> = FormField(""),
    val password: FormField<String> = FormField(""),
    val age: FormField<String> = FormField(""),
    val isSubmitting: Boolean = false,
    val successMessage: String? = null
) : UiState {
    val isFormValid: Boolean
        get() = email.isValid && password.isValid && age.isValid &&
                email.isDirty && password.isDirty && age.isDirty
}

sealed interface RegistrationAction : UiAction {
    data class EmailChanged(val email: String) : RegistrationAction
    data class PasswordChanged(val password: String) : RegistrationAction
    data class AgeChanged(val age: String) : RegistrationAction
    data object Submit : RegistrationAction
}
```

### 2. ViewModel with Validation

```kotlin
class RegistrationViewModel : BaseViewModel<RegistrationState, RegistrationAction>(
    RegistrationState()
) {
    override suspend fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.EmailChanged -> updateState {
                copy(email = email.setValue(action.email, Validators.required(), Validators.email()))
            }
            is RegistrationAction.PasswordChanged -> updateState {
                copy(password = password.setValue(
                    action.password,
                    Validators.required(),
                    Validators.minLength(8, "Password must be at least 8 characters")
                ))
            }
            is RegistrationAction.AgeChanged -> updateState {
                copy(age = age.setValue(
                    action.age,
                    Validators.required(),
                    Validators.digitsRequired("Age must be a number")
                ))
            }
            RegistrationAction.Submit -> {
                // Mark all fields touched to surface errors
                val touchedState = state.value.copy(
                    email = state.value.email.markTouched(Validators.required(), Validators.email()),
                    password = state.value.password.markTouched(
                        Validators.required(), Validators.minLength(8)
                    ),
                    age = state.value.age.markTouched(
                        Validators.required(), Validators.digitsRequired()
                    )
                )

                if (touchedState.isFormValid) {
                    updateState { touchedState.copy(isSubmitting = true) }
                    // Submit to API...
                    updateState { copy(isSubmitting = false, successMessage = "Registered!") }
                } else {
                    updateState { touchedState }
                }
            }
        }
    }
}
```

### 3. Compose UI

```kotlin
@Composable
fun RegistrationScreen(viewModel: RegistrationViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Email field
        OutlinedTextField(
            value = state.email.value,
            onValueChange = { viewModel.handleAction(RegistrationAction.EmailChanged(it)) },
            label = { Text("Email") },
            isError = state.email.isTouched && state.email.isInvalid
        )
        if (state.email.isTouched && state.email.isInvalid) {
            Text(
                text = state.email.errors.first(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Password field
        OutlinedTextField(
            value = state.password.value,
            onValueChange = { viewModel.handleAction(RegistrationAction.PasswordChanged(it)) },
            label = { Text("Password") },
            isError = state.password.isTouched && state.password.isInvalid
        )
        if (state.password.isTouched && state.password.isInvalid) {
            Text(
                text = state.password.errors.first(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Submit button
        Button(
            onClick = { viewModel.handleAction(RegistrationAction.Submit) },
            enabled = !state.isSubmitting
        ) {
            Text(if (state.isSubmitting) "Submitting..." else "Register")
        }
    }
}
```

## Pattern: Show Errors Only on Submit

By default, `setValue` marks the field as `isTouched = true`, so errors show immediately. If you prefer to show errors only after the user clicks "Submit":

1. **On input change:** Call `setValue` but use a custom extension that doesn't set `isTouched`, or track a separate `isSubmitAttempted` boolean in your state.
2. **On submit:** Call `markTouched(...)` on every field to surface all errors at once.
3. **In Compose:** Only render error text when `field.isTouched && field.isInvalid`.

## Pattern: Cross-field Validation

For fields that depend on each other (e.g., "confirm password"), use derived properties:

```kotlin
data class SignupState(
    val password: FormField<String> = FormField(""),
    val confirmPassword: FormField<String> = FormField("")
) : UiState {
    val passwordsMatch: Boolean
        get() = password.value == confirmPassword.value
    
    val isFormValid: Boolean
        get() = password.isValid && confirmPassword.isValid && passwordsMatch
}
```
