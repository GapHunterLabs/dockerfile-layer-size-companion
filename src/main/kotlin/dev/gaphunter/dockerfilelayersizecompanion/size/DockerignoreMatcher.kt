package dev.gaphunter.dockerfilelayersizecompanion.size

/**
 * A KNOWN, DOCUMENTED partial implementation of `.dockerignore` pattern
 * matching -- not the full Docker spec. Covers what real-world
 * `.dockerignore` files overwhelmingly use in practice:
 *
 * - Exact relative path match (`secrets.txt`).
 * - A directory name anywhere in the tree (`node_modules` also matches
 *   `packages/api/node_modules`), same convention Git/Docker users
 *   expect from a bare name with no slash.
 * - A single trailing wildcard segment: either a double-star segment
 *   (`node_modules` followed by a slash and two stars) or a single-star
 *   segment (`dist` followed by a slash and one star).
 * - A leading `*` wildcard for extensions/suffixes (`*.log`, `*.tmp`).
 * - A `!`-prefixed negation re-includes a path an earlier pattern excluded.
 *
 * Deliberately NOT implemented (documented honestly in the README,
 * never silently wrong): mid-pattern `**` globstar, `?` single-char
 * wildcard, character classes (`[abc]`), and full precedence edge
 * cases from the Moby `patternmatcher` reference implementation. A
 * `.dockerignore` that only uses the common subset above gets a
 * correct result; one that leans on advanced glob syntax may
 * over-count (treat an actually-ignored path as included) rather than
 * silently under-count -- see README "Known limitations".
 */
class DockerignoreMatcher private constructor(private val rules: List<Rule>) {

    private data class Rule(val negate: Boolean, val matcher: (String) -> Boolean)

    /** [relativePath] uses forward slashes, no leading slash, relative to the dockerignore's own directory. */
    fun isIgnored(relativePath: String): Boolean {
        val normalized = relativePath.trimStart('/')
        var ignored = false
        for (rule in rules) {
            if (rule.matcher(normalized)) {
                ignored = !rule.negate
            }
        }
        return ignored
    }

    companion object {
        val EMPTY = DockerignoreMatcher(emptyList())

        fun parse(content: String): DockerignoreMatcher {
            val rules = content.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { toRule(it) }
            return DockerignoreMatcher(rules)
        }

        private fun toRule(rawPattern: String): Rule {
            var pattern = rawPattern
            val negate = pattern.startsWith("!")
            if (negate) pattern = pattern.substring(1)
            pattern = pattern.trim().trimStart('/')

            val matcher: (String) -> Boolean = when {
                pattern.endsWith("/**") -> {
                    val prefix = pattern.removeSuffix("/**")
                    ({ path: String -> path == prefix || path.startsWith("$prefix/") })
                }
                pattern.endsWith("/*") -> {
                    val prefix = pattern.removeSuffix("/*")
                    ({ path: String ->
                        path.startsWith("$prefix/") && !path.substring(prefix.length + 1).contains('/')
                    })
                }
                pattern.startsWith("*.") -> {
                    val suffix = pattern.removePrefix("*")
                    ({ path: String -> path.endsWith(suffix) })
                }
                !pattern.contains('/') -> {
                    // Bare name, no slash: matches that path component
                    // anywhere in the tree (Git/Docker convention).
                    ({ path: String ->
                        path == pattern || path.startsWith("$pattern/") || path.contains("/$pattern/") || path.endsWith("/$pattern")
                    })
                }
                else -> {
                    val exact = pattern.trimEnd('/')
                    ({ path: String -> path == exact || path.startsWith("$exact/") })
                }
            }
            return Rule(negate, matcher)
        }
    }
}
