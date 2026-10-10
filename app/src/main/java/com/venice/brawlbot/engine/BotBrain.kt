package com.venice.brawlbot.engine

import com.venice.brawlbot.Config
import com.venice.brawlbot.vision.Decision
import com.venice.brawlbot.vision.Scene
import kotlin.math.hypot

object BotBrain {

    private var lastGadget = 0L
    private var lastHyper = 0L

    private const val LOW_HP = 0.35f
    private const val MID_HP = 0.62f

    fun decide(s: Scene): Decision {
        val d = Decision()
        if (!s.visible) return d

        val now = System.currentTimeMillis()
        val cx = Config.screenW * 0.5f
        val cy = Config.screenH * 0.5f

        val e = s.enemies.minByOrNull { hypot(it.x - cx, it.y - cy) }

        if (s.selfHp < LOW_HP) {
            if (e != null) {
                d.moveX = cx - e.x
                d.moveY = cy - e.y
            } else {
                d.moveX = 0f; d.moveY = 0f
            }
            if (s.gadgetReady && now - lastGadget > 4000) {
                d.gadget = true; lastGadget = now
            }
            if (s.ammo > 0 && e != null && e.size > 30f) d.attack = true
            return norm(d)
        }

        if (e == null) { d.moveY = -1f; return d }

        d.moveX = e.x - cx
        d.moveY = e.y - cy

        if (s.ammo > 0) d.attack = true

        if (s.superReady) {
            if (s.hyperReady && now - lastHyper > 2500) {
                d.hyper = true; lastHyper = now
            }
            d.aimSuper = Pair(e.x - cx, e.y - cy)
        }

        if (s.gadgetReady && s.selfHp < MID_HP && now - lastGadget > 6000) {
            d.gadget = true; lastGadget = now
        }

        return norm(d)
    }

    private fun norm(d: Decision): Decision {
        val n = hypot(d.moveX, d.moveY)
        if (n > 1f) { d.moveX /= n; d.moveY /= n }
        return d
    }

    fun reset() { lastGadget = 0L; lastHyper = 0L }
}