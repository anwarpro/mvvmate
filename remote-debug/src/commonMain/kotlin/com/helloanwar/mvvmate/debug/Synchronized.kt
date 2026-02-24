package com.helloanwar.mvvmate.debug

/**
 * Executes the given [block] holding the monitor on the given [lock].
 * Provides a multiplatform-friendly way to use synchronization.
 */
expect inline fun <R> synchronized(lock: Any, block: () -> R): R
