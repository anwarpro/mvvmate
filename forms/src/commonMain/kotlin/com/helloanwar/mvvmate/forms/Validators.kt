package com.helloanwar.mvvmate.forms

/**
 * A collection of common, built-in validators for form fields.
 */
public object Validators {

    /**
     * Validates that the value is not null and, if it's a String, not blank.
     */
    public fun <T> required(message: String = "Required"): Validator<T?> = { value ->
        if (value == null || (value is String && value.isBlank())) message else null
    }

    /**
     * Validates that a string's length is at least [length].
     */
    public fun minLength(length: Int, message: String = "Must be at least $length characters"): Validator<String?> = { value ->
        if (value != null && value.length < length) message else null
    }

    /**
     * Validates that a string's length is at most [length].
     */
    public fun maxLength(length: Int, message: String = "Must be at most $length characters"): Validator<String?> = { value ->
        if (value != null && value.length > length) message else null
    }

    /**
     * Validates that a string matches a standard email format.
     */
    public fun email(message: String = "Invalid email"): Validator<String?> = { value ->
        val regex = "^[A-Za-z0-9+_.-]+@(.+)\$".toRegex()
        if (value != null && value.isNotBlank() && !regex.matches(value)) message else null
    }

    /**
     * Validates that a string matches the provided [regex] pattern.
     */
    public fun pattern(regex: Regex, message: String = "Invalid format"): Validator<String?> = { value ->
        if (value != null && value.isNotBlank() && !regex.matches(value)) message else null
    }
    
    /**
     * Validates that a string contains only digits (0-9).
     */
    public fun digitsRequired(message: String = "Must contain only digits"): Validator<String?> = { value ->
        if (value != null && value.isNotBlank() && !value.all { it.isDigit() }) message else null
    }
    
    /**
     * Validates that a string represents a valid decimal number.
     */
    public fun decimalRequired(message: String = "Must be a valid decimal number"): Validator<String?> = { value ->
        if (value != null && value.isNotBlank() && value.toDoubleOrNull() == null) message else null
    }
}
