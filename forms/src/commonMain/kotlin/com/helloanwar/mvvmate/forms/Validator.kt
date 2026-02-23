package com.helloanwar.mvvmate.forms

/**
 * A functional interface for validating a single value of type [T].
 * Returns an error message if the value is invalid, or null if it is valid.
 */
public typealias Validator<T> = (T) -> String?
