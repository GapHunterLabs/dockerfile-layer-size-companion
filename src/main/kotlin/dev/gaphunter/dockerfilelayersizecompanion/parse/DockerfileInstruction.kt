package dev.gaphunter.dockerfilelayersizecompanion.parse

/**
 * One parsed Dockerfile instruction. [lineNumber] and [lineEndOffset]
 * are 0-based document coordinates for the FIRST line of the
 * instruction (where a `\`-continued instruction spans several source
 * lines, the inlay always anchors to the first line, same convention
 * as a normal editor gutter/end-of-line marker would use).
 */
data class DockerfileInstruction(
    val keyword: String,
    /** Raw argument text, continuation backslashes already joined and normalized to single spaces. */
    val argsText: String,
    val lineNumber: Int,
    val lineEndOffset: Int,
)

/**
 * A `COPY`/`ADD` instruction's arguments split into the source path(s)
 * and destination -- everything else (flags like `--from=`,
 * `--chown=`) captured separately so size resolution can inspect them
 * without re-parsing.
 */
data class CopyArgs(
    val sources: List<String>,
    val destination: String,
    /** Present when the instruction copies from a build stage (`COPY --from=builder ...`), not the local build context. */
    val fromStage: String?,
    val chown: String?,
)

/**
 * Minimal line-by-line Dockerfile scanner -- same "hand-rolled lexer for
 * a small, stable, line-oriented syntax" pattern as `NginxLexer`,
 * simpler here because a Dockerfile is a much smaller grammar than
 * nginx config: one instruction keyword per
 * (possibly `\`-continued) statement, no nesting, no braces. No
 * Grammar-Kit, no bundled Docker plugin dependency.
 */
object DockerfileParser {

    private val KNOWN_KEYWORDS = setOf(
        "FROM", "RUN", "COPY", "ADD", "WORKDIR", "ENV", "ARG", "LABEL",
        "EXPOSE", "VOLUME", "USER", "ENTRYPOINT", "CMD", "SHELL",
        "STOPSIGNAL", "HEALTHCHECK", "ONBUILD", "MAINTAINER",
    )

    fun parse(text: String): List<DockerfileInstruction> {
        val lines = text.split("\n")
        val result = mutableListOf<DockerfileInstruction>()

        var i = 0
        var offset = 0
        // Running per-line start offsets, computed once up front so we
        // never re-scan the buffer for offsets while joining continuations.
        val lineStartOffsets = IntArray(lines.size)
        for ((idx, line) in lines.withIndex()) {
            lineStartOffsets[idx] = offset
            offset += line.length + 1 // +1 for the '\n' consumed by split
        }

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++
                continue
            }

            val firstSpace = trimmed.indexOfFirst { it.isWhitespace() }
            val keywordCandidate = (if (firstSpace == -1) trimmed else trimmed.substring(0, firstSpace)).uppercase()

            if (keywordCandidate !in KNOWN_KEYWORDS) {
                // Malformed / unrecognized line (stray text, a parser
                // directive comment already filtered above, or a typo) --
                // never throw, just skip it. Honest limitation, not a crash.
                i++
                continue
            }

            val instructionStartLine = i
            val argsBuilder = StringBuilder(if (firstSpace == -1) "" else trimmed.substring(firstSpace + 1))

            // Follow `\`-line continuations, joining them into one logical
            // instruction. A line continuation is only recognized when the
            // line (after stripping a trailing comment-free end) ends with
            // a bare trailing backslash.
            var currentLine = trimmed
            while (currentLine.endsWith("\\") && i + 1 < lines.size) {
                // Drop the trailing backslash already accounted for above.
                if (argsBuilder.isNotEmpty() && argsBuilder.last() == '\\') {
                    argsBuilder.setLength(argsBuilder.length - 1)
                }
                i++
                currentLine = lines[i].trim()
                if (argsBuilder.isNotEmpty() && !argsBuilder.endsWith(" ")) argsBuilder.append(' ')
                argsBuilder.append(currentLine)
            }

            val lineEndOffset = lineStartOffsets[instructionStartLine] + lines[instructionStartLine].length

            result.add(
                DockerfileInstruction(
                    keyword = keywordCandidate,
                    argsText = argsBuilder.toString().trim(),
                    lineNumber = instructionStartLine,
                    lineEndOffset = lineEndOffset,
                ),
            )
            i++
        }

        return result
    }

    /**
     * Splits a `COPY`/`ADD` instruction's argument text into sources,
     * destination, and flags. Handles the JSON-array form
     * (`COPY ["a", "b", "dest"]`) and the plain-word form
     * (`COPY a b dest`), plus `--from=`/`--chown=` flags. Returns null
     * when the args are empty or otherwise unparseable -- callers treat
     * that as "can't compute a size", never a crash.
     */
    fun parseCopyArgs(argsText: String): CopyArgs? {
        var remaining = argsText.trim()
        if (remaining.isEmpty()) return null

        var fromStage: String? = null
        var chown: String? = null

        // Flags come before the path list, space-separated, each starting with "--".
        while (remaining.startsWith("--")) {
            val spaceIdx = remaining.indexOfFirst { it.isWhitespace() }
            val flag = if (spaceIdx == -1) remaining else remaining.substring(0, spaceIdx)
            when {
                flag.startsWith("--from=") -> fromStage = flag.removePrefix("--from=").trim('"', '\'')
                flag.startsWith("--chown=") -> chown = flag.removePrefix("--chown=")
                // Other real flags (--chmod=, --link, --exclude=) are accepted
                // and skipped; not used by size computation in v0.1.
            }
            remaining = if (spaceIdx == -1) "" else remaining.substring(spaceIdx + 1).trim()
        }

        if (remaining.isEmpty()) return null

        val words: List<String> = if (remaining.startsWith("[")) {
            parseJsonArrayForm(remaining)
        } else {
            splitRespectingQuotes(remaining)
        }

        if (words.size < 2) return null

        return CopyArgs(
            sources = words.dropLast(1),
            destination = words.last(),
            fromStage = fromStage,
            chown = chown,
        )
    }

    private fun splitRespectingQuotes(text: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote: Char? = null
        for (c in text) {
            when {
                inQuote != null -> {
                    if (c == inQuote) inQuote = null else current.append(c)
                }
                c == '"' || c == '\'' -> inQuote = c
                c.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        words.add(current.toString())
                        current.setLength(0)
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) words.add(current.toString())
        return words
    }

    private fun parseJsonArrayForm(text: String): List<String> {
        val inner = text.trim().removePrefix("[").removeSuffix("]")
        return inner.split(",").map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }
    }

    /** True when [keyword] is one this plugin ever attaches a hint to. */
    fun isSizableKeyword(keyword: String): Boolean = keyword == "COPY" || keyword == "ADD"

    fun isRunKeyword(keyword: String): Boolean = keyword == "RUN"
}
