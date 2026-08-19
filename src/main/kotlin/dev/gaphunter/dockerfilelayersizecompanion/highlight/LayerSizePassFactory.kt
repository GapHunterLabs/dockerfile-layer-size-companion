package dev.gaphunter.dockerfilelayersizecompanion.highlight

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.gaphunter.dockerfilelayersizecompanion.detect.DockerfileDetector

/**
 * Registers [LayerSizeHighlightingPass] to run after the IDE's own
 * `Pass.UPDATE_ALL` (general highlighting) pass -- same registration
 * shape as HTTP Status Code Inline Companion's `HttpStatusPassFactory` /
 * Regex Named Group Companion's `NamedGroupPassFactory`. Gate is by
 * filename via [DockerfileDetector], not by `Language`/`FileType` --
 * this plugin deliberately never registers a custom `FileType` (see
 * README "Why built this way"), so it works whatever file type the IDE
 * already assigned a Dockerfile (plain text, or another installed
 * Docker-support plugin's own file type).
 */
class LayerSizePassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {

    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(
            this,
            null,
            intArrayOf(Pass.UPDATE_ALL),
            false,
            -1,
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (editor.isOneLineMode) return null
        if (!file.isPhysical) return null
        if (!DockerfileDetector.isDockerfile(file.name)) return null
        return LayerSizeHighlightingPass(file.project, editor, file)
    }
}
