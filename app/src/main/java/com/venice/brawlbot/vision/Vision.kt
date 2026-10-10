package com.venice.brawlbot.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.venice.brawlbot.Config
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object Vision {

    private const val SW = 480
    private var SH = 270
    private var scale = 1f

    private var small: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = Rect()

    private val mat = Mat()
    private val rgb = Mat()
    private val hsv = Mat()
    private val mask = Mat()
    private val tmp = Mat()

    fun prepare(w: Int, h: Int) {
        if (small != null && src.right == w && src.bottom == h) return
        SH = (SW * h / w).coerceAtLeast(1)
        scale = w.toFloat() / SW
        small = Bitmap.createBitmap(SW, SH, Bitmap.Config.ARGB_8888)
        canvas = Canvas(small!!)
        src.set(0, 0, w, h)
        dst.set(0, 0, SW, SH)
    }

    private fun reg(fl: Float, ft: Float, fr: Float, fb: Float): Rect {
        val l = (fl * SW).toInt().coerceIn(0, SW - 1)
        val t = (ft * SH).toInt().coerceIn(0, SH - 1)
        val r = (fr * SW).toInt().coerceIn(l + 1, SW)
        val b = (fb * SH).toInt().coerceIn(t + 1, SH)
        return Rect(l, t, r, b)
    }

    private fun frac(rect: Rect, lo: Scalar, hi: Scalar): Double {
        val sub = hsv.submat(rect.top, rect.bottom, rect.left, rect.right)
        Core.inRange(sub, lo, hi, tmp)
        val f = Core.countNonZero(tmp).toDouble() / (sub.rows() * sub.cols()).coerceAtLeast(1)
        sub.release()
        return f
    }

    private fun brightness(rect: Rect): Double {
        val sub = mat.submat(rect.top, rect.bottom, rect.left, rect.right)
        val m = Core.mean(sub)
        val v = (m.`val`[0] + m.`val`[1] + m.`val`[2]) / 3.0
        sub.release()
        return v
    }

    fun analyze(frame: Bitmap): Scene {
        prepare(frame.width, frame.height)
        canvas!!.drawBitmap(frame, src, dst, paint)
        Utils.bitmapToMat(small!!, mat)
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)

        // --- враги ---
        val bt = (Config.BATTLE_T * SH).toInt().coerceIn(0, SH - 1)
        val bb = (Config.BATTLE_B * SH).toInt().coerceIn(bt + 1, SH)
        val bl = (0.03f * SW).toInt().coerceIn(0, SW - 1)
        val br = (0.97f * SW).toInt().coerceIn(bl + 1, SW)

        val roi = hsv.submat(bt, bb, bl, br)
        val m1 = Mat()
        val m2 = Mat()
        Core.inRange(roi, Scalar(0.0, 110.0, 110.0), Scalar(10.0, 255.0, 255.0), m1)
        Core.inRange(roi, Scalar(168.0, 110.0, 110.0), Scalar(180.0, 255.0, 255.0), m2)
        Core.bitwise_or(m1, m2, mask)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE)

        val enemies = ArrayList<Blip>()
        for (c in contours) {
            val a = Imgproc.contourArea(c)
            if (a < Config.enemyMinArea || a > Config.enemyMaxArea) continue
            val rc = Imgproc.boundingRect(c)
            if (rc.width.toFloat() / rc.height.coerceAtLeast(1) < 1.2f) continue
            enemies += Blip(
                ((rc.x + rc.width / 2) + bl) * scale,
                ((rc.y + rc.height / 2) + bt) * scale,
                rc.width * scale
            )
        }
        roi.release(); m1.release(); m2.release()

        val hpFrac = readHp()

        val ammoFrac = frac(reg(Config.AMMO_L, Config.AMMO_T, Config.AMMO_R, Config.AMMO_B),
            Scalar(90.0, 80.0, 140.0), Scalar(130.0, 255.0, 255.0))
        val ammo = (ammoFrac / 0.14).toInt().coerceIn(0, 3)

        val superReady = brightness(reg(Config.SUPER_L, Config.SUPER_T,
            Config.SUPER_R, Config.SUPER_B)) > 95
        val hyperReady = frac(reg(Config.HYPER_L, Config.HYPER_T,
            Config.HYPER_R, Config.HYPER_B),
            Scalar(140.0, 70.0, 150.0), Scalar(175.0, 255.0, 255.0)) > 0.05
        val gadgetReady = brightness(reg(Config.GADGET_L, Config.GADGET_T,
            Config.GADGET_R, Config.GADGET_B)) > 90

        val visible = brightness(reg(0.80f, 0.76f, 0.95f, 0.90f)) > 45

        return Scene(enemies, hpFrac, ammo, superReady, hyperReady, gadgetReady, visible)
    }

    private fun readHp(): Float {
        val hpRect = reg(0.30f, 0.36f, 0.70f, 0.66f)
        val gmask = Mat()
        val sub = hsv.submat(hpRect.top, hpRect.bottom, hpRect.left, hpRect.right)
        Core.inRange(sub, Scalar(35.0, 90.0, 90.0), Scalar(85.0, 255.0, 255.0), gmask)
        var result = 1f
        if (Core.countNonZero(gmask) > 12) {
            val cols = Mat()
            Core.reduce(gmask, cols, 0, Core.REDUCE_MAX)
            var cnt = 0
            for (i in 0 until cols.cols()) if (cols.get(0, i)[0] > 0.0) cnt++
            result = (cnt / Config.maxHealthBarPx).coerceIn(0f, 1f)
            cols.release()
        }
        gmask.release(); sub.release()
        return result
    }
}