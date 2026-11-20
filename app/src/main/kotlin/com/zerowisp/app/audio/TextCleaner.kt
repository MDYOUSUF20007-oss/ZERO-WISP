package com.zerowisp.app.audio

/**
 * Instant rule-based transcript cleanup. No local LLM required.
 *
 * Removes:
 *  - Whisper non-speech markers: [BLANK_AUDIO], [Music], (music), [inaudible], [Silence], etc.
 *  - Standalone filler words: um, uh, er, ah, you know
 *  - Stray thinking-pause ellipses
 *  - Extra whitespace
 * Applies:
 *  - Sentence capitalization (first letter of each sentence)
 *  - Space normalization
 */
object TextCleaner {

    // Non-speech markers (case-insensitive)
    private val MARKER_PATTERN = Regex(
        """[\[\(](BLANK_AUDIO|Music|music|inaudible|Inaudible|Silence|silence|noise|applause|laughter|MUSIC|SILENCE|INAUDIBLE|♪[^♪]*♪?)[\]\)]""",
        RegexOption.IGNORE_CASE
    )

    // Standalone filler words (whole-word match, case-insensitive)
    private val FILLER_PATTERN = Regex(
        """(?<![a-zA-Z])(um+|uh+|er+|ah+|you know)(?![a-zA-Z])""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    // Standalone ellipsis (3+ dots with possible spaces)
    private val ELLIPSIS_PATTERN = Regex("""(\s*\.{2,}\s*)""")

    // Multiple spaces
    private val MULTI_SPACE_PATTERN = Regex("""[ \t]+""")

    // Sentence start (after . ! ? followed by space)
    private val SENTENCE_END_PATTERN = Regex("""([.!?]\s+)([a-z])""")

    fun clean(raw: String, cleanFillers: Boolean = true): String {
        var text = raw

        // 1. Remove non-speech markers
        text = MARKER_PATTERN.replace(text, " ")

        // 2. Remove filler words (if enabled)
        if (cleanFillers) {
            text = FILLER_PATTERN.replace(text, " ")
        }

        // 3. Collapse stray ellipses
        text = ELLIPSIS_PATTERN.replace(text, " ")

        // 4. Normalize whitespace
        text = MULTI_SPACE_PATTERN.replace(text, " ")
        text = text.trim()

        // 5. Fix sentence capitalization
        text = text.replaceFirstChar { it.uppercaseChar() }
        text = SENTENCE_END_PATTERN.replace(text) { mr ->
            mr.groupValues[1] + mr.groupValues[2].uppercase()
        }

        // 6. Remove leading/trailing punctuation artifacts
        text = text.trim(',', ' ')

        return text
    }
}
