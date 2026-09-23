package dev.gaphunter.dockerfilelayersizecompanion.size

import dev.gaphunter.dockerfilelayersizecompanion.parse.DockerfileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LayerSizeCalculatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `computes real size of a single copied file`() {
        val contextDir = tempFolder.root
        File(contextDir, "package.json").writeText("x".repeat(100))

        val copyArgs = DockerfileParser.parseCopyArgs("package.json .")!!
        val result = LayerSizeCalculator.compute(copyArgs, contextDir, DockerignoreMatcher.EMPTY)

        assertTrue(result is LayerSizeResult.Computed)
        assertEquals(100L, (result as LayerSizeResult.Computed).bytes)
        assertEquals(false, result.dockerignoreApplied)
    }

    @Test
    fun `computes recursive size of a copied directory`() {
        val contextDir = tempFolder.root
        val srcDir = File(contextDir, "src").apply { mkdirs() }
        File(srcDir, "a.txt").writeText("a".repeat(50))
        val nested = File(srcDir, "nested").apply { mkdirs() }
        File(nested, "b.txt").writeText("b".repeat(75))

        val copyArgs = DockerfileParser.parseCopyArgs("src/ /app/src/")!!
        val result = LayerSizeCalculator.compute(copyArgs, contextDir, DockerignoreMatcher.EMPTY)

        assertTrue(result is LayerSizeResult.Computed)
        assertEquals(125L, (result as LayerSizeResult.Computed).bytes)
    }

    @Test
    fun `dockerignore excludes matching paths from the computed size`() {
        val contextDir = tempFolder.root
        val nodeModules = File(contextDir, "node_modules").apply { mkdirs() }
        File(nodeModules, "big-lib.js").writeText("x".repeat(10_000))
        File(contextDir, "index.js").writeText("y".repeat(50))

        val copyArgs = DockerfileParser.parseCopyArgs(". /app")!!
        val dockerignore = DockerignoreMatcher.parse("node_modules")
        val result = LayerSizeCalculator.compute(copyArgs, contextDir, dockerignore)

        assertTrue(result is LayerSizeResult.Computed)
        val computed = result as LayerSizeResult.Computed
        // Only index.js (50 bytes) should be counted -- node_modules excluded.
        assertEquals(50L, computed.bytes)
        assertEquals(true, computed.dockerignoreApplied)
    }

    @Test
    fun `copy --from a build stage is reported as not calculable, never a fabricated number`() {
        val copyArgs = DockerfileParser.parseCopyArgs("--from=builder /app/dist /app/dist")!!
        val result = LayerSizeCalculator.compute(copyArgs, tempFolder.root, DockerignoreMatcher.EMPTY)

        assertEquals(LayerSizeResult.FromBuildStage, result)
    }

    @Test
    fun `wildcard source is reported as unresolved, never a fabricated number`() {
        val copyArgs = DockerfileParser.parseCopyArgs("*.jar /app/")!!
        val result = LayerSizeCalculator.compute(copyArgs, tempFolder.root, DockerignoreMatcher.EMPTY)

        assertEquals(LayerSizeResult.UnresolvedWildcard, result)
    }

    @Test
    fun `missing source is reported honestly, never a fabricated number`() {
        val copyArgs = DockerfileParser.parseCopyArgs("does-not-exist.txt /app/")!!
        val result = LayerSizeCalculator.compute(copyArgs, tempFolder.root, DockerignoreMatcher.EMPTY)

        assertEquals(LayerSizeResult.SourceNotFound, result)
    }

    @Test
    fun `ADD from a URL is reported as not calculable, not as a missing local file`() {
        // ADD (unlike COPY) can download a source over the network --
        // real Docker feature. That's not a file missing on disk, it's
        // genuinely not sizable without fetching it, same honesty class
        // as --from=<stage>, so it must not be reported as SourceNotFound.
        val copyArgs = DockerfileParser.parseCopyArgs("https://example.com/installer.tar.gz /tmp/")!!
        val result = LayerSizeCalculator.compute(copyArgs, tempFolder.root, DockerignoreMatcher.EMPTY)

        assertEquals(LayerSizeResult.FromUrl, result)
    }

    @Test
    fun `formatBytes uses binary units`() {
        assertEquals("500 B", LayerSizeCalculator.formatBytes(500))
        assertEquals("1.0 KB", LayerSizeCalculator.formatBytes(1024))
        assertEquals("2.0 MB", LayerSizeCalculator.formatBytes(1024L * 1024 * 2))
    }
}
