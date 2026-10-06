package com.example.hoode_app.ui.common

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.DecelerateInterpolator
import com.example.hoode_app.R
import kotlinx.coroutines.delay

/** A single cold-start reveal; session restoration runs concurrently in MainActivity. */
class LaunchIntro(activity: Activity, firstCreation: Boolean) {
    private val overlay = activity.layoutInflater.inflate(R.layout.view_launch_intro, null)
    private val startedAt = SystemClock.uptimeMillis()
    private val motion = firstCreation && ValueAnimator.areAnimatorsEnabled() &&
        !(activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled

    init {
        activity.addContentView(overlay, ViewGroup.LayoutParams(-1, -1))
        if (motion) {
            overlay.findViewById<View>(R.id.introBrand).apply {
                alpha = 0f
                translationY = 18 * resources.displayMetrics.density
                animate().alpha(1f).translationY(0f).setDuration(600)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    suspend fun reveal(content: View) {
        if (motion) delay((750 - (SystemClock.uptimeMillis() - startedAt)).coerceAtLeast(0))
        content.visibility = View.VISIBLE
        if (motion) {
            overlay.animate().alpha(0f).setDuration(220).withEndAction { remove() }.start()
        } else remove()
    }

    fun remove() {
        overlay.animate().cancel()
        overlay.findViewById<View>(R.id.introBrand).animate().cancel()
        (overlay.parent as? ViewGroup)?.removeView(overlay)
    }
}
