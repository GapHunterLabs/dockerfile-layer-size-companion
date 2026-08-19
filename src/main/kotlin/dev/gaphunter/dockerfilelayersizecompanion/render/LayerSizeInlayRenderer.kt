package dev.gaphunter.dockerfilelayersizecompanion.render

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Renders one Dockerfile Layer Size Companion inlay, pinned to the end
 * of the instruction's first line -- same after-line-end anchoring as
 * Regex Named Group Companion's `NamedGroupInlayRenderer`, chosen over
 * a gutter icon (see README "Why an inlay, not a gutter icon" for the
 * full reasoning: no gutter-icon mechanism has shipped in this catalog
 * yet, while this exact end-of-line inlay pattern has shipped twice
 * already this session with 6/6 verifyPlugin, and a `COPY`/`RUN` line
 * is naturally a single full statement -- there's no risk of two hints
 * competing for the same line the way inline literal-anchored hints
 * would need to disambiguate).
 *
 * [isWarning] swaps the flat neutral color used for a real computed
 * size to a warning-toned color for a known-expensive `RUN` pattern --
 * a deliberate visual distinction so a real measurement is never
 * confused at a glance with "this is flagged, not measured" (the same
 * distinction the README/CHANGELOG insist on in prose is also carried
 * into the rendering itself).
 */
class LayerSizeInlayRenderer(private val text: String, private val isWarning: Boolean) : EditorCustomElementRenderer {

    companion object {
        private const val LEFT_PADDING_PX = 12
        private const val ALPHA = 140
        private val WARNING_COLOR = Color(196, 128, 32)
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val fontMetrics = editor.contentComponent.getFontMetrics(font)
        return LEFT_PADDING_PX + fontMetrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(EditorFontType.ITALIC)
        g.font = font
        g.color = if (isWarning) {
            Color(WARNING_COLOR.red, WARNING_COLOR.green, WARNING_COLOR.blue, ALPHA)
        } else {
            val foreground = editor.colorsScheme.defaultForeground
            Color(foreground.red, foreground.green, foreground.blue, ALPHA)
        }
        val fontMetrics = g.getFontMetrics(font)
        val baseline = targetRegion.y + fontMetrics.ascent
        g.drawString(text, targetRegion.x + LEFT_PADDING_PX, baseline)
    }
}
