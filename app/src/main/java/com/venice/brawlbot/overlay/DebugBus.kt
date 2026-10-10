package com.venice.brawlbot.overlay

import com.venice.brawlbot.vision.Decision
import com.venice.brawlbot.vision.Scene

object DebugBus {
    @Volatile var scene: Scene? = null
    @Volatile var decision: Decision? = null
    @Volatile var status: String = ""
    @Volatile var retreating: Boolean = false
    @Volatile var view: DebugView? = null

    fun push(s: Scene, d: Decision, statusText: String) {
        scene = s; decision = d; status = statusText
        retreating = s.selfHp in 0.01f..0.35f
        view?.postInvalidate()
    }
}