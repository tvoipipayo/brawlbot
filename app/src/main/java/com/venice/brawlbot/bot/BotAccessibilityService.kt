package com.venice.brawlbot.bot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: BotAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    private suspend fun gesture(path: Path, ms: Long): Boolean =
        suspendCancellableCoroutine { cont ->
            val stroke = GestureDescription.StrokeDescription(path, 0, ms.coerceAtLeast(30))
            val g = GestureDescription.Builder().addStroke(stroke).build()
            val ok = dispatchGesture(g, object : GestureResultCallback() {
                override fun onCompleted(d: GestureDescription?) { if (cont.isActive) cont.resume(true) }
                override fun onCancelled(d: GestureDescription?) { if (cont.isActive) cont.resume(false) }
            }, null)
            if (!ok && cont.isActive) cont.resume(false)
        }

    suspend fun tap(x: Float, y: Float, ms: Long = 60) {
        gesture(Path().apply { moveTo(x, y) }, ms)
    }

    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        gesture(Path().apply { moveTo(x1, y1); lineTo(x2, y2) }, ms)
    }
}