package com.venice.brawlbot.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.venice.brawlbot.Config
import kotlin.math.hypot

class DebugView(ctx: Context) : View(ctx) {

    private val box = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 4f
        color = Color.rgb(0, 255, 120); isAntiAlias = true
    }
    private val aim = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 4f
        color = Color.YELLOW; isAntiAlias = true
    }
    private val fill = Paint().apply { color = Color.argb(120, 255, 60, 60) }
    private val txt = Paint().apply {
        color = Color.WHITE; textSize = 34f; isAntiAlias = true
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }
    private val txt2 = Paint().apply {
        color = Color.rgb(120, 255, 160); textSize = 30f; isAntiAlias = true
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        val s = DebugBus.scene ?: return
        val cx = Config.screenW * 0.5f
        val cy = Config.screenH * 0.5f

        if (DebugBus.retreating) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        }

        for (e in s.enemies) {
            val r = (e.size / 2f).coerceIn(26f, 140f)
            canvas.drawCircle(e.x, e.y, r, box)
        }

        s.enemies.minByOrNull { hypot(it.x - cx, it.y - cy) }?.let { t ->
            canvas.drawCircle(t.x, t.y, 18f, aim)
            canvas.drawLine(cx, cy, t.x, t.y, aim)
        }

        var y = 70f
        canvas.drawText("HP: ${(s.selfHp * 100).toInt()}%", 40f, y, txt2); y += 40f
        canvas.drawText("AMMO: ${s.ammo}/3", 40f, y, txt2); y += 40f
        canvas.drawText("SUPER: ${if (s.superReady) "READY" else "-"}", 40f, y, txt2); y += 40f
        canvas.drawText("HYPER: ${if (s.hyperReady) "READY" else "-"}", 40f, y, txt2); y += 40f
        canvas.drawText("GADGET: ${if (s.gadgetReady) "READY" else "-"}", 40f, y, txt2); y += 40f
        canvas.drawText(DebugBus.status, 40f, y, txt)
    }
}