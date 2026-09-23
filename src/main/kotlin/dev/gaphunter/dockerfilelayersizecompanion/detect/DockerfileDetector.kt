package dev.gaphunter.dockerfilelayersizecompanion.detect

/**
 * Decides whether a file is a Dockerfile, by name only -- deliberately
 * conservative, same "opt-in, never hijack a generic extension" spirit
 * as `NginxConfigDetector`. Unlike nginx config (where `.conf` is a
 * generic extension shared by many tools), Dockerfile naming is already
 * narrow and standard, so a content sniff isn't needed: any exact
 * match on `Dockerfile`/`dockerfile`, an optional multi-stage suffix
 * (`Dockerfile.dev`, `Dockerfile.prod`), or the `.dockerfile` extension
 * (Dockerfile.dockerfile / worker.dockerfile) is treated as one.
 */
object DockerfileDetector {

    private val EXACT_NAMES = setOf("dockerfile")

    /**
     * A trailing extension here means the file is prose/documentation
     * (a cheatsheet, a notes file) that just happens to start with
     * "dockerfile." -- not a variant Dockerfile anyone actually builds
     * (real variants look like `Dockerfile.dev`/`Dockerfile.prod`,
     * never `Dockerfile.md`). This plugin does real filesystem I/O
     * (sizes what a COPY/ADD line references, relative to the file's
     * own directory), so misdetecting a doc file means resolving and
     * sizing paths against a directory that has nothing to do with a
     * real build context.
     */
    private val DOCUMENTATION_SUFFIXES = setOf("md", "markdown", "mdx", "rst", "adoc")

    fun isDockerfile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        if (lower in EXACT_NAMES) return true
        if (lower.endsWith(".dockerfile")) return true
        // "Dockerfile.dev", "Dockerfile.prod", "Dockerfile.ci" -- common
        // multi-environment naming convention this plugin should still
        // light up on.
        if (lower.startsWith("dockerfile.")) {
            return lower.substringAfterLast('.') !in DOCUMENTATION_SUFFIXES
        }
        return false
    }
}
