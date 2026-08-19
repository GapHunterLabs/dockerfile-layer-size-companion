package dev.gaphunter.dockerfilelayersizecompanion.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

/** One inlay to render: where it anchors, its text, and whether it's a warning (known-expensive RUN) or a real measurement (COPY/ADD size). */
data class LayerSizeInlayEntry(val offset: Int, val text: String, val isWarning: Boolean)

/**
 * Owns the lifecycle of this plugin's inlays for a given editor, same
 * pattern as Regex Named Group Companion's `NamedGroupInlayManager`:
 * every pass run calls [replaceInlays] with a fresh set of entries --
 * the previous set is always disposed first, so re-running the daemon
 * (constantly, while typing) replaces stale hints instead of piling
 * new ones on top.
 */
object LayerSizeInlayManager {

    private val INLAYS_KEY = Key.create<MutableList<Inlay<*>>>("dev.gaphunter.dockerfilelayersizecompanion.inlays")

    /** Must be called on the EDT -- true for every real caller (`doApplyInformationToEditor`) and for tests. */
    fun replaceInlays(editor: Editor, entries: List<LayerSizeInlayEntry>) {
        editor.getUserData(INLAYS_KEY)?.forEach { inlay ->
            if (inlay.isValid) Disposer.dispose(inlay)
        }

        val created = entries.mapNotNull { entry ->
            editor.inlayModel.addAfterLineEndElement(
                entry.offset,
                false,
                LayerSizeInlayRenderer(entry.text, entry.isWarning),
            )
        }
        editor.putUserData(INLAYS_KEY, created.toMutableList())
    }
}
