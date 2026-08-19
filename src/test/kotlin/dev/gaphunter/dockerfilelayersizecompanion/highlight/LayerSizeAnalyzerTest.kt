package dev.gaphunter.dockerfilelayersizecompanion.highlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LayerSizeAnalyzerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `end-to-end - real file COPY gets a real size hint`() {
        val contextDir = tempFolder.root
        File(contextDir, "app.jar").writeText("x".repeat(2048))

        val text = "FROM eclipse-temurin:21\nCOPY app.jar /app/app.jar\n"
        val entries = LayerSizeAnalyzer.analyze(text, contextDir, dockerignoreContent = null)

        assertEquals(1, entries.size)
        assertTrue(entries[0].text.contains("2.0 KB"))
        assertEquals(false, entries[0].isWarning)
    }

    @Test
    fun `end-to-end - RUN with known-expensive pattern is flagged as a warning, no number`() {
        val text = "FROM debian:12\nRUN apt-get install -y curl\n"
        val entries = LayerSizeAnalyzer.analyze(text, tempFolder.root, dockerignoreContent = null)

        assertEquals(1, entries.size)
        assertTrue(entries[0].isWarning)
        assertTrue(entries[0].text.contains("known-expensive pattern"))
        // Never a fabricated size number for RUN.
        assertTrue(!Regex("""\d+(\.\d+)?\s*(B|KB|MB|GB)""").containsMatchIn(entries[0].text))
    }

    @Test
    fun `end-to-end - clean RUN produces no hint at all`() {
        val text = "FROM debian:12\nRUN echo hello\n"
        val entries = LayerSizeAnalyzer.analyze(text, tempFolder.root, dockerignoreContent = null)

        assertEquals(0, entries.size)
    }

    @Test
    fun `end-to-end - multi-stage COPY --from does not crash and is labeled honestly`() {
        val text = """
            FROM golang:1.22 AS builder
            RUN go build -o /out/app .
            FROM alpine:3.19
            COPY --from=builder /out/app /usr/local/bin/app
        """.trimIndent()

        val entries = LayerSizeAnalyzer.analyze(text, tempFolder.root, dockerignoreContent = null)

        assertEquals(1, entries.size)
        assertTrue(entries[0].text.contains("not calculable"))
    }

    @Test
    fun `end-to-end - malformed Dockerfile does not crash`() {
        val text = """
            FROM node:20
            !!! this is garbage %%%
            COPY
            RUN
        """.trimIndent()

        // Must not throw for any of these lines.
        val entries = LayerSizeAnalyzer.analyze(text, tempFolder.root, dockerignoreContent = null)
        assertTrue(entries.size <= 2)
    }

    @Test
    fun `end-to-end - dockerignore excludes node_modules from a wildcard-free COPY of the whole context`() {
        val contextDir = tempFolder.root
        val nodeModules = File(contextDir, "node_modules").apply { mkdirs() }
        File(nodeModules, "lib.js").writeText("x".repeat(5000))
        File(contextDir, "index.js").writeText("y".repeat(20))

        val text = "FROM node:20\nCOPY . /app\n"
        val entries = LayerSizeAnalyzer.analyze(text, contextDir, dockerignoreContent = "node_modules\n")

        assertEquals(1, entries.size)
        assertTrue(entries[0].text.contains("respects .dockerignore"))
        assertTrue(!entries[0].text.contains("4.") ) // sanity: not the ~5KB unfiltered total
    }

    @Test
    fun `end-to-end - consecutive RUN instructions are flagged as combinable`() {
        val text = """
            FROM debian:12
            RUN echo one
            RUN echo two
            RUN echo three
        """.trimIndent()

        val entries = LayerSizeAnalyzer.analyze(text, tempFolder.root, dockerignoreContent = null)

        assertTrue(entries.any { it.text.contains("could be combined") })
    }
}
