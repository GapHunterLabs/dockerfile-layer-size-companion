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
}
