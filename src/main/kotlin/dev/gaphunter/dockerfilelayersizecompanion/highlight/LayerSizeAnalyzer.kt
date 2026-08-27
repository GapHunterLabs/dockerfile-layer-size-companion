package dev.gaphunter.dockerfilelayersizecompanion.highlight

import dev.gaphunter.dockerfilelayersizecompanion.expensive.ExpensiveRunPatterns
import dev.gaphunter.dockerfilelayersizecompanion.parse.DockerfileInstruction
import dev.gaphunter.dockerfilelayersizecompanion.parse.DockerfileParser
import dev.gaphunter.dockerfilelayersizecompanion.render.LayerSizeInlayEntry
import dev.gaphunter.dockerfilelayersizecompanion.size.DockerignoreMatcher
import dev.gaphunter.dockerfilelayersizecompanion.size.LayerSizeCalculator
import dev.gaphunter.dockerfilelayersizecompanion.size.LayerSizeResult
import java.io.File

/**
 * Pure, platform-free core: takes Dockerfile text + the real build
 * context directory (+ optional `.dockerignore` content) and produces
 * the list of inlay entries to render. Deliberately has zero
 * dependency on `Editor`/`PsiFile`/the EDT, so every case (real size,
 * `.dockerignore` exclusion, `--from=` stage copy, wildcard, missing
 * source, expensive RUN pattern, malformed line) is unit-testable
 * directly against real `java.io.File`s in a temp directory -- same
 * off-EDT/on-EDT split contract as every other highlighting pass in
 * this catalog: heavy computation off the EDT.
 */
object LayerSizeAnalyzer {

    fun analyze(text: String, buildContextDir: File, dockerignoreContent: String?): List<LayerSizeInlayEntry> {
        val instructions = DockerfileParser.parse(text)
        val dockerignore = dockerignoreContent?.let { DockerignoreMatcher.parse(it) } ?: DockerignoreMatcher.EMPTY

        val entries = mutableListOf<LayerSizeInlayEntry>()

        for (instruction in instructions) {
            if (DockerfileParser.isSizableKeyword(instruction.keyword)) {
                entries.add(analyzeCopyOrAdd(instruction, buildContextDir, dockerignore))
            } else if (DockerfileParser.isRunKeyword(instruction.keyword)) {
                analyzeRun(instruction)?.let { entries.add(it) }
            }
        }

        for (group in ExpensiveRunPatterns.findCombinableRuns(instructions)) {
            val last = group.last()
            // Attach the "could be combined" note to the last RUN in the
            // consecutive group -- appended, not replacing any per-RUN
            // expensive-pattern hint already added above for that same line.
            val existingIndex = entries.indexOfFirst { it.offset == last.lineEndOffset }
            val note = "known-expensive pattern: ${group.size} consecutive RUN instructions could be combined into one to save layer overhead"
            if (existingIndex >= 0) {
                val existing = entries[existingIndex]
                entries[existingIndex] = existing.copy(text = "${existing.text} | $note")
            } else {
                entries.add(LayerSizeInlayEntry(last.lineEndOffset, " $note", isWarning = true))
            }
        }

        return entries
    }

    private fun analyzeCopyOrAdd(
        instruction: DockerfileInstruction,
        buildContextDir: File,
        dockerignore: DockerignoreMatcher,
    ): LayerSizeInlayEntry {
        val copyArgs = DockerfileParser.parseCopyArgs(instruction.argsText)
        if (copyArgs == null) {
            return LayerSizeInlayEntry(instruction.lineEndOffset, " unparseable", isWarning = false)
        }

        val result = LayerSizeCalculator.compute(copyArgs, buildContextDir, dockerignore)
        val text = when (result) {
            is LayerSizeResult.Computed -> {
                val suffix = if (result.dockerignoreApplied) " (respects .dockerignore)" else ""
                " ~${LayerSizeCalculator.formatBytes(result.bytes)}$suffix"
            }
            LayerSizeResult.FromBuildStage -> " copies from a previous build stage -- not calculable without running the build"
            LayerSizeResult.UnresolvedWildcard -> " wildcard source -- not calculable in v0.1"
            LayerSizeResult.SourceNotFound -> " source not found in build context"
            LayerSizeResult.Unparseable -> " unparseable"
        }
        return LayerSizeInlayEntry(instruction.lineEndOffset, text, isWarning = false)
    }

    private fun analyzeRun(instruction: DockerfileInstruction): LayerSizeInlayEntry? {
        val reasons = ExpensiveRunPatterns.check(instruction)
        if (reasons.isEmpty()) return null
        val summary = if (reasons.size == 1) reasons.first() else "${reasons.size} known-expensive patterns detected"
        return LayerSizeInlayEntry(instruction.lineEndOffset, " known-expensive pattern: $summary", isWarning = true)
    }
}
