package com.helloanwar.mvvmate.core

/**
 * Typed error hierarchy for MVVMate applications.
 *
 * Provides structured error classification instead of raw strings,
 * enabling better error handling, retry logic, and user-facing messages.
 *
 * All variants expose a [message] property for backward-compatible string access.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null,
    open val code: String? = null
) {
    /**
     * Network-related errors (HTTP failures, connectivity issues).
     *
     * @property httpCode HTTP status code, if available.
     * @property isRetryable Whether this error is safe to retry automatically.
     */
    data class Network(
        override val message: String,
        val httpCode: Int? = null,
        val isRetryable: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Operation exceeded the allowed time limit.
     *
     * @property durationMs The timeout duration in milliseconds.
     */
    data class Timeout(
        override val message: String,
        val durationMs: Long = 0L,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Input validation errors.
     *
     * @property field The field that failed validation, if applicable.
     */
    data class Validation(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Catch-all for unclassified errors.
     */
    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    companion object {
        /**
         * Classify a generic [Throwable] into the appropriate [AppError] subtype.
         */
        fun from(throwable: Throwable): AppError {
            return when (throwable) {
                is kotlinx.coroutines.TimeoutCancellationException ->
                    Timeout(message = "Operation timed out", cause = throwable)
                else ->
                    Unknown(message = throwable.message ?: "Unknown Error", cause = throwable)
            }
        }
    }
}
