package com.helloanwar.mvvmate.forms

/**
 * Represents a single field within a form, containing the current value, validation errors,
 * and metadata like whether it has been touched or modified.
 *
 * @param T The type of the field's value.
 */
public data class FormField<T>(
    val value: T,
    val errors: List<String> = emptyList(),
    val isTouched: Boolean = false,
    val isDirty: Boolean = false
) {
    /** true if there are no validation errors. */
    public val isValid: Boolean get() = errors.isEmpty()
    
    /** true if there are one or more validation errors. */
    public val isInvalid: Boolean get() = errors.isNotEmpty()
}

/**
 * Updates the value of the FormField, applies the given validators, and marks it as dirty and touched.
 * 
 * @param newValue The new value for the field.
 * @param validators The validation rules to apply to the new value.
 * @return A new FormField reflecting the updated state.
 */
public fun <T> FormField<T>.setValue(
    newValue: T, 
    vararg validators: Validator<T>
): FormField<T> {
    val newErrors = validators.mapNotNull { it(newValue) }
    return copy(
        value = newValue,
        errors = newErrors,
        isDirty = true,
        isTouched = true
    )
}

/**
 * Marks the field as touched without changing its value, and optionally runs validators.
 * 
 * @param validators The validation rules to apply.
 * @return A new FormField reflecting the updated state.
 */
public fun <T> FormField<T>.markTouched(
    vararg validators: Validator<T>
): FormField<T> {
    return copy(
        isTouched = true,
        errors = validators.mapNotNull { it(value) }
    )
}
