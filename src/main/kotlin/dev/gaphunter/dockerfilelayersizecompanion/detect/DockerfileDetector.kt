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

    fun isDockerfile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        if (lower in EXACT_NAMES) return true
        if (lower.endsWith(".dockerfile")) return true
        // "Dockerfile.dev", "Dockerfile.prod", "Dockerfile.ci" -- common
        // multi-environment naming convention this plugin should still
        // light up on.
        if (lower.startsWith("dockerfile.")) return true
        return false
    }
}
