package com.helloanwar.mvvmate.core

/**
 * Platform interface representing platform-specific implementations.
 *
 * This interface is expected to be implemented by each target in a Kotlin Multiplatform project (e.g., Android, iOS).
 * It is typically used to get platform-specific information like the name of the platform.
 */
interface Platform {
    /**
     * The name of the platform (e.g., "Android", "iOS").
     */
    val name: String
}


/**
 * Function to get the platform-specific implementation of [Platform].
 *
 * This function is expected to be implemented separately in each platform's source set.
 *
 * @return A [Platform] instance that represents the current platform.
 */
expect fun getPlatform(): Platform