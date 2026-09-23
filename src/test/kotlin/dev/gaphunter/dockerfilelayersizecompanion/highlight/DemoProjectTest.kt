package dev.gaphunter.dockerfilelayersizecompanion.highlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The demo project as a tester actually opens it: `demo/Dockerfile`
 * read from disk, with `demo/` itself as the real build context, so
 * the walkthrough in `demo/README.md` cannot drift away from what the
 * analyzer does.
 */
class DemoProjectTest {

    @Test
    fun `the demo Dockerfile produces the exact hints the walkthrough describes`() {
        val demoDir = File("demo")
        val text = File(demoDir, "Dockerfile").readText()
        val dockerignore = File(demoDir, ".dockerignore").readText()

        val entries = LayerSizeAnalyzer.analyze(text, demoDir, dockerignore)
        val byLineText = entries.associate { it.offset to it.text }

        // node_modules/src COPY lines get real sizes.
        assertTrue(entries.any { !it.isWarning && it.text.contains("~") })

        // apt-get update && install curl (no --no-install-recommends, no
        // apt-lists cleanup) is flagged with its 2 legitimate reasons --
        // neither is the curl-invocation one (fixed in 0.2.0: installing
        // curl is not downloading with it).
        val aptWarning = entries.first { it.isWarning && it.text.contains("2 known-expensive patterns") }
        assertTrue(!aptWarning.text.contains("cleanup"))

        // Installing curl cleanly (0.2.0 addition) must NOT be flagged at
        // all -- exactly the one RUN warning above, nothing else.
        assertEquals(1, entries.count { it.isWarning })

        // ADD from a URL is "not calculable", never "source not found".
        assertTrue(entries.any { it.text.contains("not calculable without fetching") })
        assertTrue(entries.none { it.text.contains("source not found") })

        // Multi-stage COPY --from is still recognized honestly.
        assertTrue(entries.any { it.text.contains("previous build stage") })

        assertEquals(byLineText.size, entries.size) // sanity: no two hints silently collided on the same offset
    }
}
