package dev.gaphunter.dockerfilelayersizecompanion.review

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader

/**
 * Asks the user to rate the plugin on Marketplace, once, after the
 * plugin's own analysis has flagged a real number of *distinct*
 * known-expensive-pattern warnings -- never on install, never on a
 * timer, and never inflated by the platform re-running the same
 * highlighting pass over an unchanged file (a `TextEditorHighlightingPass`
 * fires on every background highlight pass, not just on a real edit).
 * [recordHit] takes a small stable key per finding (e.g.
 * `"$filePath:$offset"`) and only counts it the first time that exact
 * key is ever seen, so re-highlighting the same file repeatedly
 * doesn't move the counter. Real `Computed`/`FromBuildStage`/etc. size
 * entries never count -- only a genuine `isWarning` finding does, same
 * "earn the ask" principle as the rest of the catalog.
 *
 * Never re-asks once the user has either rated or dismissed it.
 * Persisted via [PropertiesComponent] at the application level (not
 * per-project) -- how many distinct problems this plugin has flagged
 * isn't tied to any one project, and neither is whether the user
 * already answered.
 */
object ReviewPrompt {

    /** How many distinct real findings before the prompt shows once. */
    private const val HITS_BEFORE_PROMPT = 10

    /** Caps how many dedupe keys are retained -- well past HITS_BEFORE_PROMPT, just a sane upper bound. */
    private const val MAX_TRACKED_KEYS = 500

    private const val KEY_SEEN_FINDINGS = "dev.gaphunter.dockerfilelayersizecompanion.review.seenFindings"
    private const val KEY_ANSWERED = "dev.gaphunter.dockerfilelayersizecompanion.review.answered"

    private const val NOTIFICATION_GROUP_ID = "Dockerfile Layer Size Companion"

    private const val MARKETPLACE_URL =
        "https://plugins.jetbrains.com/plugin/33674-dockerfile-layer-size-companion/reviews"

    /**
     * Call this from the real detection code path once per distinct
     * real finding, passing a key that's stable for that same finding
     * across re-highlighting the same unchanged file (a file path +
     * offset is enough -- doesn't need to be globally unique or
     * survive the finding moving to a different line). Safe to call on
     * any thread.
     */
    fun recordHit(project: Project?, dedupeKey: String) {
        val properties = PropertiesComponent.getInstance()
        if (properties.getBoolean(KEY_ANSWERED)) return

        val seen = properties.getList(KEY_SEEN_FINDINGS)?.toMutableSet() ?: mutableSetOf()
        if (!seen.add(dedupeKey)) return // already counted this exact finding

        if (seen.size > MAX_TRACKED_KEYS) {
            // Drop to just the count once the tracked set gets large --
            // avoids unbounded growth in a huge project; the milestone
            // has long since passed by MAX_TRACKED_KEYS anyway.
            properties.unsetValue(KEY_SEEN_FINDINGS)
        } else {
            properties.setList(KEY_SEEN_FINDINGS, seen)
        }

        if (seen.size == HITS_BEFORE_PROMPT) {
            showPrompt(project)
        }
    }

    private fun showPrompt(project: Project?) {
        val properties = PropertiesComponent.getInstance()

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                "Dockerfile Layer Size Companion",
                "If this plugin has been useful, a rating on the Marketplace helps other developers find it.",
                NotificationType.INFORMATION,
            )
        notification.setIcon(IconLoader.getIcon("/META-INF/pluginIcon.svg", ReviewPrompt::class.java))

        notification.addAction(NotificationAction.createSimpleExpiring("Rate on Marketplace") {
            properties.setValue(KEY_ANSWERED, true)
            BrowserUtil.browse(MARKETPLACE_URL)
        })
        notification.addAction(NotificationAction.createSimpleExpiring("Don't ask again") {
            properties.setValue(KEY_ANSWERED, true)
        })

        notification.notify(project)
    }
}
