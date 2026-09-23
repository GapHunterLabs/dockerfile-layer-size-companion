package dev.gaphunter.dockerfilelayersizecompanion.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockerfileDetectorTest {

    @Test
    fun `recognizes exact Dockerfile name, case-insensitive`() {
        assertTrue(DockerfileDetector.isDockerfile("Dockerfile"))
        assertTrue(DockerfileDetector.isDockerfile("dockerfile"))
    }

    @Test
    fun `recognizes Dockerfile with environment suffix`() {
        assertTrue(DockerfileDetector.isDockerfile("Dockerfile.dev"))
        assertTrue(DockerfileDetector.isDockerfile("Dockerfile.prod"))
    }

    @Test
    fun `recognizes dockerfile extension`() {
        assertTrue(DockerfileDetector.isDockerfile("worker.dockerfile"))
    }

    @Test
    fun `does not match unrelated files`() {
        assertFalse(DockerfileDetector.isDockerfile("docker-compose.yml"))
        assertFalse(DockerfileDetector.isDockerfile("README.md"))
        assertFalse(DockerfileDetector.isDockerfile("Makefile"))
    }

    @Test
    fun `does not treat a markdown cheatsheet named Dockerfile-dot-md as a real Dockerfile`() {
        // A "dockerfile.md" cheatsheet is documentation, not a real
        // build file -- and unlike a plain warning, this plugin does
        // real filesystem I/O (stats files a COPY/ADD line references
        // relative to wherever that file sits), so misdetecting it
        // means resolving and sizing paths against a random directory
        // that has nothing to do with a real build context.
        assertFalse(DockerfileDetector.isDockerfile("Dockerfile.md"))
        assertFalse(DockerfileDetector.isDockerfile("dockerfile.md"))
        assertFalse(DockerfileDetector.isDockerfile("Dockerfile.markdown"))
    }
}
