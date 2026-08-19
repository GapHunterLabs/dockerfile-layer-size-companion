package dev.gaphunter.dockerfilelayersizecompanion.render

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Exercises the real `InlayModel.addAfterLineEndElement` / `Disposer`
 * calls -- same de-risking pattern as Regex Named Group Companion's
 * `NamedGroupInlayManagerTest` / HTTP Status Code Inline Companion's
 * `HttpStatusInlayManagerTest`. Does NOT verify actual on-screen
 * pixels (see README "Known limitations").
 */
class LayerSizeInlayManagerTest : BasePlatformTestCase() {

    fun testReplaceInlaysAddsOneInlayPerEntry() {
        myFixture.configureByText("Dockerfile", "FROM node:20\nCOPY app.jar /app/app.jar\n")
        val editor = myFixture.editor
        val document = editor.document

        LayerSizeInlayManager.replaceInlays(
            editor,
            listOf(
                LayerSizeInlayEntry(document.getLineEndOffset(0), " base image", isWarning = false),
                LayerSizeInlayEntry(document.getLineEndOffset(1), " ~2.0 KB", isWarning = false),
            ),
        )

        val inlays = editor.inlayModel.getAfterLineEndElementsInRange(0, document.textLength)
        assertEquals(2, inlays.size)
    }

    fun testReplaceInlaysDisposesPreviousInlaysInsteadOfAccumulating() {
        myFixture.configureByText("Dockerfile", "FROM node:20\nRUN echo hi\n")
        val editor = myFixture.editor
        val document = editor.document

        LayerSizeInlayManager.replaceInlays(editor, listOf(LayerSizeInlayEntry(document.getLineEndOffset(1), " a", false)))
        LayerSizeInlayManager.replaceInlays(editor, listOf(LayerSizeInlayEntry(document.getLineEndOffset(1), " b", true)))

        val inlays = editor.inlayModel.getAfterLineEndElementsInRange(0, document.textLength)
        assertEquals(1, inlays.size)
    }

    fun testReplaceInlaysWithEmptyListClearsExistingOnes() {
        myFixture.configureByText("Dockerfile", "FROM node:20\n")
        val editor = myFixture.editor
        val document = editor.document

        LayerSizeInlayManager.replaceInlays(editor, listOf(LayerSizeInlayEntry(document.getLineEndOffset(0), " x", false)))
        LayerSizeInlayManager.replaceInlays(editor, emptyList())

        val inlays = editor.inlayModel.getAfterLineEndElementsInRange(0, document.textLength)
        assertEquals(0, inlays.size)
    }
}
