package com.helloanwar.mvvmate.core

import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * A logger that records timestamped events into a chronological timeline.
 *
 * Useful for debugging complex action sequences, understanding execution order,
 * and post-mortem analysis of state changes.
 *
 * ```kotlin
 * // Enable:
 * val timeline = TimelineLogger()
 * MvvMate.logger = timeline
 *
 * // After reproducing the issue:
 * timeline.dump()       // Prints full history
 * timeline.dumpLast(10) // Prints last 10 entries
 * timeline.clear()      // Reset
 * ```
 *
 * Can also be combined with another logger:
 * ```kotlin
 * val timeline = TimelineLogger(delegate = PrintLogger)
 * ```
 */
class TimelineLogger(
    /**
     * Optional delegate logger. When set, all events are forwarded to this
     * logger in addition to being recorded in the timeline.
     */
    private val delegate: MvvMateLogger? = null,

    /**
     * Maximum number of entries to retain. Oldest entries are evicted
     * when the limit is reached. Default: 500.
     */
    private val maxEntries: Int = 500
) : MvvMateLogger {

    /**
     * A single timestamped entry in the timeline.
     */
    data class Entry(
        val relativeMs: Long,
        val type: EntryType,
        val viewModel: String,
        val message: String
    ) {
        fun format(): String {
            val icon = type.icon
            return "[+${relativeMs}ms] $icon $viewModel :: $message"
        }
    }

    enum class EntryType(val icon: String) {
        ACTION("▶"),
        STATE("🔄"),
        EFFECT("⚡"),
        ERROR("❌"),
        NETWORK("🌐")
    }

    private val entries = mutableListOf<Entry>()
    private val timeSource = TimeSource.Monotonic
    private var startMark: ComparableTimeMark = timeSource.markNow()

    private fun record(type: EntryType, viewModel: String, message: String) {
        val relativeMs = startMark.elapsedNow().inWholeMilliseconds
        val entry = Entry(
            relativeMs = relativeMs,
            type = type,
            viewModel = viewModel,
            message = message
        )
        entries.add(entry)
        if (entries.size > maxEntries) {
            entries.removeAt(0)
        }
    }

    // --- MvvMateLogger implementation ---

    override fun logAction(viewModelName: String, action: UiAction) {
        record(EntryType.ACTION, viewModelName, action::class.simpleName ?: action.toString())
        delegate?.logAction(viewModelName, action)
    }

    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {
        val diffSummary = StateDiffUtil.diffSummary(oldState, newState)
        record(EntryType.STATE, viewModelName, diffSummary)
        delegate?.logStateChange(viewModelName, oldState, newState)
    }

    override fun logEffect(viewModelName: String, effect: Any) {
        record(EntryType.EFFECT, viewModelName, "${effect::class.simpleName}: $effect")
        delegate?.logEffect(viewModelName, effect)
    }

    override fun logError(viewModelName: String, error: Throwable, context: String) {
        val ctx = if (context.isNotEmpty()) " [$context]" else ""
        record(EntryType.ERROR, viewModelName, "${error::class.simpleName}$ctx: ${error.message}")
        delegate?.logError(viewModelName, error, context)
    }

    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {
        val det = if (details.isNotEmpty()) " — $details" else ""
        record(EntryType.NETWORK, "Network[$tag]", "${phase.name}$det")
        delegate?.logNetwork(tag, phase, details)
    }

    // --- Timeline operations ---

    /**
     * Get all recorded entries.
     */
    fun getEntries(): List<Entry> = entries.toList()

    /**
     * Get the last [n] entries.
     */
    fun getLastEntries(n: Int): List<Entry> = entries.takeLast(n)

    /**
     * Get all entries for a specific ViewModel.
     */
    fun getEntriesFor(viewModelName: String): List<Entry> = entries.filter { it.viewModel == viewModelName }

    /**
     * Print the full timeline to stdout.
     */
    fun dump() {
        val snapshot = getEntries()
        if (snapshot.isEmpty()) {
            println("[MVVMate Timeline] (empty)")
            return
        }
        println("[MVVMate Timeline] ${snapshot.size} entries:")
        println("─".repeat(60))
        snapshot.forEach { println(it.format()) }
        println("─".repeat(60))
    }

    /**
     * Print the last [n] entries to stdout.
     */
    fun dumpLast(n: Int) {
        val snapshot = getLastEntries(n)
        if (snapshot.isEmpty()) {
            println("[MVVMate Timeline] (empty)")
            return
        }
        val total = entries.size
        println("[MVVMate Timeline] last $n of $total entries:")
        println("─".repeat(60))
        snapshot.forEach { println(it.format()) }
        println("─".repeat(60))
    }

    /**
     * Format the timeline as a multi-line string (useful for crash reports).
     */
    fun formatAsString(): String = buildString {
        val snapshot = getEntries()
        appendLine("[MVVMate Timeline] ${snapshot.size} entries:")
        snapshot.forEach { appendLine(it.format()) }
    }

    /**
     * Clear all entries and reset the start time.
     */
    fun clear() {
        entries.clear()
        startMark = timeSource.markNow()
    }
}
