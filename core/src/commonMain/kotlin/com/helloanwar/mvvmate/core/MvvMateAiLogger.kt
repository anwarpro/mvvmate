package com.helloanwar.mvvmate.core

/**
 * An AI-focused logger that records app events into a ring buffer (circular queue).
 * It retains the last [maxHistorySize] events in memory, allowing you to instantly 
 * extract a complete grammatical timeline of what happened right before a crash.
 * 
 * Includes a [PrivacyRedactor] to prevent sensitive data from leaking into the
 * JSON/Text traces.
 * 
 * Note: This logger is designed to be sequence-safe when accessed primarily from the
 * Main thread, as is standard for ViewModel Action/State updates.
 *
 * @param delegate An optional secondary logger (e.g. PrintLogger or Firebase Crashlytics).
 * @param maxHistorySize The maximum number of events to retain in memory. 
 *        50-100 is usually plenty to establish context without consuming noticeable RAM.
 * @param redactor A privacy scrubber used to redact sensitive fields before outputting to trace.
 */
class MvvMateAiLogger(
    private val delegate: MvvMateLogger = NoOpLogger,
    private val maxHistorySize: Int = 50,
    private val redactor: PrivacyRedactor = RegexPrivacyRedactor()
) : MvvMateLogger {

    private val eventBuffer = ArrayDeque<AiLogEvent>(maxHistorySize)
    private var sequenceCounter: Long = 0L

    /**
     * Extracts the current ring buffer as a [CrashTrace].
     * 
     * @return A copy of the recorded trail of events up to this moment.
     */
    fun takeSnapshot(): CrashTrace {
        return CrashTrace(eventBuffer.toList())
    }

    /**
     * Extracts a human-readable and AI-readable digest of the recent events.
     * All output is filtered through the configured [PrivacyRedactor].
     */
    fun takeRedactedSnapshotString(): String {
        val trace = takeSnapshot()
        return redactor.redact(trace.formatReadableDigest())
    }

    private fun addEvent(event: AiLogEvent) {
        if (eventBuffer.size >= maxHistorySize) {
            eventBuffer.removeFirst()
        }
        eventBuffer.addLast(event)
    }

    private fun getNextSequenceId(): Long {
        return ++sequenceCounter
    }

    override fun logAction(viewModelName: String, action: UiAction) {
        addEvent(
            AiLogEvent.Action(
                sequenceId = getNextSequenceId(),
                viewModelName = viewModelName,
                actionClass = action::class.simpleName ?: "Unknown",
                actionDetails = action.toString()
            )
        )
        delegate.logAction(viewModelName, action)
    }

    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {
        val diff = StateDiffUtil.diffSummary(oldState, newState)
        addEvent(
            AiLogEvent.StateChange(
                sequenceId = getNextSequenceId(),
                viewModelName = viewModelName,
                diffSummary = diff,
                fullStateDump = newState.toString()
            )
        )
        delegate.logStateChange(viewModelName, oldState, newState)
    }

    override fun logEffect(viewModelName: String, effect: Any) {
        addEvent(
            AiLogEvent.Effect(
                sequenceId = getNextSequenceId(),
                viewModelName = viewModelName,
                effectClass = effect::class.simpleName ?: "Unknown",
                effectDetails = effect.toString()
            )
        )
        delegate.logEffect(viewModelName, effect)
    }

    override fun logError(viewModelName: String, error: Throwable, context: String) {
        addEvent(
            AiLogEvent.Error(
                sequenceId = getNextSequenceId(),
                viewModelName = viewModelName,
                exceptionClass = error::class.simpleName ?: "Unknown",
                errorMessage = error.message,
                context = context
            )
        )
        delegate.logError(viewModelName, error, context)
    }

    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {
        addEvent(
            AiLogEvent.Network(
                sequenceId = getNextSequenceId(),
                tag = tag,
                phase = phase.name,
                details = details
            )
        )
        delegate.logNetwork(tag, phase, details)
    }
}
