package com.venice.brawlbot.capture

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import com.venice.brawlbot.Config

class ScreenCaptureService : Service() {

    companion object {
        @Volatile private var frame: Bitmap? = null
        @Volatile private var w = 0
        @Volatile private var h = 0
        fun latest(): Bitmap? = frame
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
    }

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, buildNotification())
        val code = intent?.getIntExtra(EXTRA_CODE, -1) ?: return START_STICKY
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_STICKY
        start(code, data)
        return START_STICKY
    }

    private fun start(code: Int, data: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val metrics = resources.displayMetrics
        w = metrics.widthPixels; h = metrics.heightPixels
        Config.screenW = w; Config.screenH = h

        projection = mgr.getMediaProjection(code, data)
        reader = ImageReader.newInstance(w, h, android.graphics.PixelFormat.RGBA_8888, 2)
        display = projection!!.createVirtualDisplay(
            "brawlbot", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, null
        )
        reader!!.setOnImageAvailableListener({ r -> grab(r) }, null)
    }

    private fun grab(r: ImageReader) {
        val img: Image = try { r.acquireLatestImage() } catch (e: Exception) { null } ?: return
        try {
            val plane = img.planes[0]
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val padding = rowStride - pixelStride * w
            val bmp = Bitmap.createBitmap(w + padding / pixelStride, h, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(plane.buffer)
            frame = Bitmap.createBitmap(bmp, 0, 0, w, h)
        } finally { img.close() }
    }

    private fun buildNotification(): Notification {
        val ch = "brawlbot_cap"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(ch) == null)
            nm.createNotificationChannel(
                NotificationChannel(ch, "Захват", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, ch)
            .setContentTitle("BrawlBot")
            .setContentText("Захват экрана активен")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop(); super.onDestroy()
    }
}