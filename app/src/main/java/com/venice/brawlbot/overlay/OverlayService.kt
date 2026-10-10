package com.venice.brawlbot.overlay

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import com.venice.brawlbot.R
import com.venice.brawlbot.data.QueueRepository

class OverlayService : Service() {

    companion object {
        @Volatile var instance: OverlayService? = null
        var visible = false
    }

    private lateinit var wm: WindowManager
    private var root: View? = null
    private var debug: DebugView? = null
    private var controller: ModMenuController? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        QueueRepository.init(this)
        startForeground(1, notif())
        show()
    }

    private fun show() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        root = LayoutInflater.from(this).inflate(R.layout.overlay_menu, FrameLayout(this))
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        p.x = 40; p.y = 120
        wm.addView(root, p)
        visible = true
        controller = ModMenuController(this, root!!, wm, p)
        controller?.bind()
    }

    fun toggleDebug() { if (debug == null) showDebug() else hideDebug() }

    private fun showDebug() {
        val v = DebugView(this)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(v, p)
        debug = v
        DebugBus.view = v
    }

    private fun hideDebug() {
        debug?.let { wm.removeView(it) }
        debug = null; DebugBus.view = null
    }

    fun hide() { root?.let { wm.removeView(it) }; root = null; visible = false }

    private fun notif(): Notification {
        val ch = "brawlbot_ov"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(ch) == null)
            nm.createNotificationChannel(
                NotificationChannel(ch, "Меню", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, ch)
            .setContentTitle("BrawlBot")
            .setContentText("Мод-меню активно")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
    }

    override fun onDestroy() { hideDebug(); hide(); instance = null; super.onDestroy() }
}