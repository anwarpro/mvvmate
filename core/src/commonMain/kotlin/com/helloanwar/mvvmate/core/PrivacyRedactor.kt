package com.helloanwar.mvvmate.core

/**
 * Interface defining a mechanism to scrub or redact sensitive information from
 * strings before they are logged or exported (e.g. for AI crash dumps).
 */
interface PrivacyRedactor {
    /**
     * @param input The raw string that might contain sensitive data.
     * @return The sanitized string with sensitive data obfuscated.
     */
    fun redact(input: String): String
}

/**
 * A pass-through redactor that performs no redaction. Not recommended for production.
 */
object NoOpRedactor : PrivacyRedactor {
    override fun redact(input: String): String = input
}

/**
 * A Privacy Redactor that uses Regex patterns to sanitize common sensitive data points.
 */
class RegexPrivacyRedactor(
    private val patterns: List<Pair<Regex, String>> = DefaultPatterns()
) : PrivacyRedactor {

    override fun redact(input: String): String {
        var sanitized = input
        for ((regex, replacement) in patterns) {
            sanitized = regex.replace(sanitized, replacement)
        }
        return sanitized
    }

    companion object {
        /**
         * A set of common default redaction patterns.
         */
        @Suppress("RegExpRedundantEscape", "RegExpUnnecessaryNonCapturingGroup")
        fun DefaultPatterns(): List<Pair<Regex, String>> = listOf(
            // Redact basic emails (john.doe@example.com -> j***@e***.com)
            Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") to "[EMAIL_REDACTED]",
            
            // Redact exact password/token/secret matching in toString() fields
            // e.g. password="MySecretPassword123" -> password="[REDACTED]"
            Regex("(?i)(password|token|secret|auth|bearer)\\s*(=|:)\\s*[\"']?([^\\s,}'\"]+)[\"']?") 
                    to "$1=[REDACTED]",

            // Redact credit cards (simple 13-19 digit contiguous number detection)
            // (Note: this is a naive match and might catch other long numbers, prioritizing safety)
            Regex("\\b(?:\\d[ -]*?){13,19}\\b") to "[CREDIT_CARD_REDACTED]"
        )
    }
}
