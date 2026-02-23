package com.helloanwar.mvvmate.formsexample

import com.helloanwar.mvvmate.core.UiState
import com.helloanwar.mvvmate.forms.FormField
import com.helloanwar.mvvmate.forms.Validators
import com.helloanwar.mvvmate.forms.markTouched
import com.helloanwar.mvvmate.forms.setValue
import com.helloanwar.mvvmate.core.BaseViewModel
import com.helloanwar.mvvmate.core.UiAction
import kotlinx.coroutines.delay

data class RegistrationState(
    val fullName: FormField<String> = FormField(""),
    val email: FormField<String> = FormField(""),
    val age: FormField<String> = FormField(""),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
) : UiState {
    val isFormValid: Boolean
        get() = fullName.isValid && email.isValid && age.isValid
}

sealed interface RegistrationAction : UiAction {
    data class FullNameChanged(val value: String) : RegistrationAction
    data class EmailChanged(val value: String) : RegistrationAction
    data class AgeChanged(val value: String) : RegistrationAction
    data object Submit : RegistrationAction
    data object Reset : RegistrationAction
}

class RegistrationViewModel : BaseViewModel<RegistrationState, RegistrationAction>(RegistrationState()) {

    override suspend fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.FullNameChanged -> updateState {
                copy(fullName = fullName.setValue(
                    action.value, 
                    Validators.required("Name is required"),
                    Validators.minLength(3, "Name must be at least 3 characters")
                ))
            }
            is RegistrationAction.EmailChanged -> updateState {
                copy(email = email.setValue(
                    action.value,
                    Validators.required("Email is required"),
                    Validators.email("Invalid email format")
                ))
            }
            is RegistrationAction.AgeChanged -> updateState {
                copy(age = age.setValue(
                    action.value,
                    Validators.required("Age is required"),
                    Validators.digitsRequired("Age must be a number"),
                    Validators.maxLength(3, "Age is too long")
                ))
            }
            RegistrationAction.Submit -> submitForm()
            RegistrationAction.Reset -> updateState { RegistrationState() }
        }
    }

    private suspend fun submitForm() {
        val currentState = state.value
        
        // Touch all fields to show any existing validation errors when Submit is clicked
        updateState {
            copy(
                fullName = fullName.markTouched(
                    Validators.required("Name is required"),
                    Validators.minLength(3, "Name must be at least 3 characters")
                ),
                email = email.markTouched(
                    Validators.required("Email is required"), 
                    Validators.email("Invalid email format")
                ),
                age = age.markTouched(
                    Validators.required("Age is required"),
                    Validators.digitsRequired("Age must be a number"),
                    Validators.maxLength(3, "Age is too long")
                )
            )
        }

        // Re-evaluate form validity
        if (state.value.isFormValid) {
            updateState { copy(isSubmitting = true) }
            // Simulate network request
            delay(1500)
            updateState { copy(isSubmitting = false, isSuccess = true) }
        }
    }
}
