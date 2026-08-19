package dev.gaphunter.dockerfilelayersizecompanion.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockerfileParserTest {

    @Test
    fun `parses simple instructions`() {
        val text = """
            FROM node:20
            WORKDIR /app
            COPY package.json .
            RUN npm install
        """.trimIndent()

        val instructions = DockerfileParser.parse(text)

        assertEquals(4, instructions.size)
        assertEquals("FROM", instructions[0].keyword)
        assertEquals("node:20", instructions[0].argsText)
        assertEquals("COPY", instructions[2].keyword)
        assertEquals("package.json .", instructions[2].argsText)
        assertEquals("RUN", instructions[3].keyword)
        assertEquals("npm install", instructions[3].argsText)
    }

    @Test
    fun `skips comments and blank lines`() {
        val text = """
            # This is a comment
            FROM node:20

            # Another comment
            RUN echo hi
        """.trimIndent()

        val instructions = DockerfileParser.parse(text)

        assertEquals(2, instructions.size)
        assertEquals("FROM", instructions[0].keyword)
        assertEquals("RUN", instructions[1].keyword)
    }

    @Test
    fun `joins backslash line continuations into one instruction`() {
        val text = "RUN apt-get update && \\\n    apt-get install -y curl && \\\n    rm -rf /var/lib/apt/lists/*"

        val instructions = DockerfileParser.parse(text)

        assertEquals(1, instructions.size)
        assertTrue(instructions[0].argsText.contains("apt-get update"))
        assertTrue(instructions[0].argsText.contains("apt-get install -y curl"))
        assertTrue(instructions[0].argsText.contains("rm -rf /var/lib/apt/lists/*"))
    }

    @Test
    fun `does not crash on malformed or unrecognized lines`() {
        val text = """
            FROM node:20
            this is not a real instruction at all
            RUN echo hi
            !!! garbage line %%%
        """.trimIndent()

        val instructions = DockerfileParser.parse(text)

        // Only the two real instructions are recognized; malformed lines
        // are skipped, never thrown.
        assertEquals(2, instructions.size)
        assertEquals("FROM", instructions[0].keyword)
        assertEquals("RUN", instructions[1].keyword)
    }

    @Test
    fun `handles empty file without crashing`() {
        val instructions = DockerfileParser.parse("")
        assertEquals(0, instructions.size)
    }

    @Test
    fun `keyword matching is case-insensitive`() {
        val text = "from node:20\ncopy a b"
        val instructions = DockerfileParser.parse(text)
        assertEquals("FROM", instructions[0].keyword)
        assertEquals("COPY", instructions[1].keyword)
    }

    @Test
    fun `parseCopyArgs splits plain word form`() {
        val args = DockerfileParser.parseCopyArgs("src/ dest/")
        assertEquals(listOf("src/"), args?.sources)
        assertEquals("dest/", args?.destination)
        assertNull(args?.fromStage)
    }

    @Test
    fun `parseCopyArgs splits JSON array form`() {
        val args = DockerfileParser.parseCopyArgs("""["a.txt", "b.txt", "/dest/"]""")
        assertEquals(listOf("a.txt", "b.txt"), args?.sources)
        assertEquals("/dest/", args?.destination)
    }

    @Test
    fun `parseCopyArgs extracts --from stage flag`() {
        val args = DockerfileParser.parseCopyArgs("--from=builder /app/dist /app/dist")
        assertEquals("builder", args?.fromStage)
        assertEquals(listOf("/app/dist"), args?.sources)
        assertEquals("/app/dist", args?.destination)
    }

    @Test
    fun `parseCopyArgs extracts --chown flag`() {
        val args = DockerfileParser.parseCopyArgs("--chown=appuser:appgroup src dest")
        assertEquals("appuser:appgroup", args?.chown)
        assertEquals(listOf("src"), args?.sources)
    }

    @Test
    fun `parseCopyArgs returns null for empty args`() {
        assertNull(DockerfileParser.parseCopyArgs(""))
        assertNull(DockerfileParser.parseCopyArgs("   "))
    }

    @Test
    fun `parseCopyArgs returns null for single word with no destination`() {
        assertNull(DockerfileParser.parseCopyArgs("onlyonepath"))
    }
}
