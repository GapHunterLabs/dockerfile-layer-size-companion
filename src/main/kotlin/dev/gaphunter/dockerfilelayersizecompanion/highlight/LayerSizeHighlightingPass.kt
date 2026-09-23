package dev.gaphunter.dockerfilelayersizecompanion.highlight

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.gaphunter.dockerfilelayersizecompanion.render.LayerSizeInlayEntry
import dev.gaphunter.dockerfilelayersizecompanion.render.LayerSizeInlayManager
import dev.gaphunter.dockerfilelayersizecompanion.review.ReviewPrompt
import java.io.File

/**
 * Runs [LayerSizeAnalyzer] against [file]'s real text and its real
 * on-disk parent directory (the build context, v0.1 assumption
 * documented in README "Known limitations"), then hands the resulting
 * entries to [LayerSizeInlayManager]. Same off-EDT/on-EDT split
 * contract as every other highlighting pass in this catalog:
 * [doCollectInformation] runs off the EDT and only reads immutable
 * PSI/filesystem state; [doApplyInformationToEditor] runs on the EDT
 * and is the only place that touches [Editor]/`InlayModel`.
 */
class LayerSizeHighlightingPass(
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
) : TextEditorHighlightingPass(project, editor.document, false) {

    private var entries: List<LayerSizeInlayEntry> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val virtualFile = file.virtualFile ?: return
        val dockerfileIoFile = File(virtualFile.path)
        val buildContextDir = dockerfileIoFile.parentFile ?: return

        val dockerignoreFile = File(buildContextDir, ".dockerignore")
        val dockerignoreContent = if (dockerignoreFile.isFile) {
            runCatching { dockerignoreFile.readText() }.getOrNull()
        } else {
            null
        }

        entries = LayerSizeAnalyzer.analyze(editor.document.text, buildContextDir, dockerignoreContent)
    }

    override fun doApplyInformationToEditor() {
        LayerSizeInlayManager.replaceInlays(editor, entries)

        val virtualFile = file.virtualFile ?: return
        for (entry in entries) {
            if (entry.isWarning) {
                ReviewPrompt.recordHit(project, "${virtualFile.path}:${entry.offset}")
            }
        }
    }
}
