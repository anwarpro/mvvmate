package com.helloanwar.mvvmate.core

/**
 * Pluggable logging interface for MVVMate.
 *
 * Implement this interface to integrate with your preferred logging
 * framework (Timber, Napier, co.touchlab.kermit, println, etc.).
 *
 * Set your logger via [MvvMate.logger].
 */
interface MvvMateLogger {

    /**
     * Called when an action is dispatched to a ViewModel.
     */
    fun logAction(viewModelName: String, action: UiAction)

    /**
     * Called when state changes in a ViewModel.
     */
    fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState)

    /**
     * Called when a side effect is emitted.
     */
    fun logEffect(viewModelName: String, effect: Any)

    /**
     * Called when an error occurs during action processing.
     */
    fun logError(viewModelName: String, error: Throwable, context: String = "")

    /**
     * Called during network call lifecycle events.
     */
    fun logNetwork(tag: String, phase: NetworkPhase, details: String = "")

    /**
     * Phases of a network call lifecycle.
     */
    enum class NetworkPhase {
        START, RETRY, SUCCESS, FAILURE, CANCEL, TIMEOUT
    }
}

/**
 * Default no-op logger that discards all log messages.
 */
object NoOpLogger : MvvMateLogger {
    override fun logAction(viewModelName: String, action: UiAction) {}
    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {}
    override fun logEffect(viewModelName: String, effect: Any) {}
    override fun logError(viewModelName: String, error: Throwable, context: String) {}
    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {}
}

/**
 * Simple print-based logger for development and debugging.
 * Logs all events to standard output with a `[MVVMate]` prefix.
 */
object PrintLogger : MvvMateLogger {
    override fun logAction(viewModelName: String, action: UiAction) {
        println("[MVVMate] ▶ $viewModelName :: ${action::class.simpleName}")
    }

    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {
        val diff = StateDiffUtil.diffSummary(oldState, newState)
        println("[MVVMate] 🔄 $viewModelName :: $diff")
    }

    override fun logEffect(viewModelName: String, effect: Any) {
        println("[MVVMate] ⚡ $viewModelName :: ${effect::class.simpleName} → $effect")
    }

    override fun logError(viewModelName: String, error: Throwable, context: String) {
        val ctx = if (context.isNotEmpty()) " [$context]" else ""
        println("[MVVMate] ❌ $viewModelName$ctx :: ${error::class.simpleName}: ${error.message}")
    }

    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {
        val icon = when (phase) {
            MvvMateLogger.NetworkPhase.START -> "🌐"
            MvvMateLogger.NetworkPhase.RETRY -> "🔄"
            MvvMateLogger.NetworkPhase.SUCCESS -> "✅"
            MvvMateLogger.NetworkPhase.FAILURE -> "❌"
            MvvMateLogger.NetworkPhase.CANCEL -> "🚫"
            MvvMateLogger.NetworkPhase.TIMEOUT -> "⏱️"
        }
        val det = if (details.isNotEmpty()) " — $details" else ""
        println("[MVVMate] $icon Network [$tag] ${phase.name}$det")
    }
}

/**
 * Global configuration singleton for MVVMate.
 *
 * ```kotlin
 * // In your Application.onCreate() or main():
 * MvvMate.logger = PrintLogger  // or your custom logger
 * MvvMate.isDebug = BuildConfig.DEBUG
 * ```
 */
object MvvMate {
    /**
     * The active logger. Set to [PrintLogger] for development
     * or implement [MvvMateLogger] for custom integrations.
     * Default: [NoOpLogger] (no logging).
     */
    var logger: MvvMateLogger = NoOpLogger

    /**
     * Enable debug mode for more verbose logging.
     * When false, state change logging is suppressed for performance.
     */
    var isDebug: Boolean = false

    /**
     * Interface for the remote debugger to interact with a ViewModel.
     */
    interface DebugBridge {
        /** Restore a specific state snapshot. */
        fun restoreState(state: UiState)
        /** Inject an action from a string payload. */
        fun injectAction(payload: String)
    }

    /**
     * Registry of ViewModel debug bridges.
     *
     * ViewModels auto-register here when [isDebug] is true.
     * The [RemoteDebugLogger][com.helloanwar.mvvmate.debug.RemoteDebugLogger]
     * uses this to restore state and inject actions.
     *
     * Key = ViewModel logTag.
     */
    val debugBridge: MutableMap<String, DebugBridge> = mutableMapOf()
}
