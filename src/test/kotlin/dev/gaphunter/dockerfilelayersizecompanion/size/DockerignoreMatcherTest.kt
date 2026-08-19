package dev.gaphunter.dockerfilelayersizecompanion.size

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockerignoreMatcherTest {

    @Test
    fun `bare name matches anywhere in the tree`() {
        val matcher = DockerignoreMatcher.parse("node_modules")
        assertTrue(matcher.isIgnored("node_modules"))
        assertTrue(matcher.isIgnored("node_modules/some/nested/file.js"))
        assertTrue(matcher.isIgnored("packages/api/node_modules"))
        assertFalse(matcher.isIgnored("src/index.js"))
    }

    @Test
    fun `double star suffix matches directory and everything under it`() {
        val matcher = DockerignoreMatcher.parse("dist/**")
        assertTrue(matcher.isIgnored("dist"))
        assertTrue(matcher.isIgnored("dist/bundle.js"))
        assertTrue(matcher.isIgnored("dist/nested/file.js"))
        assertFalse(matcher.isIgnored("src/dist.js"))
    }

    @Test
    fun `single star suffix matches only direct children`() {
        val matcher = DockerignoreMatcher.parse("dist/*")
        assertTrue(matcher.isIgnored("dist/bundle.js"))
        assertFalse(matcher.isIgnored("dist/nested/file.js"))
    }

    @Test
    fun `extension wildcard matches by suffix`() {
        val matcher = DockerignoreMatcher.parse("*.log")
        assertTrue(matcher.isIgnored("app.log"))
        assertTrue(matcher.isIgnored("logs/app.log"))
        assertFalse(matcher.isIgnored("app.log.txt"))
    }

    @Test
    fun `negation re-includes a path`() {
        val matcher = DockerignoreMatcher.parse("*.log\n!important.log")
        assertTrue(matcher.isIgnored("debug.log"))
        assertFalse(matcher.isIgnored("important.log"))
    }

    @Test
    fun `comments and blank lines are ignored`() {
        val matcher = DockerignoreMatcher.parse("# comment\n\nnode_modules\n")
        assertTrue(matcher.isIgnored("node_modules"))
    }

    @Test
    fun `empty matcher ignores nothing`() {
        assertFalse(DockerignoreMatcher.EMPTY.isIgnored("anything"))
    }

    @Test
    fun `exact path with slash matches only that path and its children`() {
        val matcher = DockerignoreMatcher.parse("secrets/prod.env")
        assertTrue(matcher.isIgnored("secrets/prod.env"))
        assertFalse(matcher.isIgnored("secrets/dev.env"))
    }
}
