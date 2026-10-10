package com.venice.brawlbot.engine

import android.graphics.Bitmap
import com.venice.brawlbot.Config
import com.venice.brawlbot.bot.BotAccessibilityService
import com.venice.brawlbot.capture.ScreenCaptureService
import com.venice.brawlbot.data.BrawlerTask
import com.venice.brawlbot.data.QueueRepository
import com.venice.brawlbot.overlay.DebugBus
import com.venice.brawlbot.vision.Decision
import com.venice.brawlbot.vision.Ocr
import com.venice.brawlbot.vision.Vision
import kotlinx.coroutines.*

object BotEngine {

    private enum class Phase { IDLE, SELECT, READ_TROPHIES, PRESS_PLAY, IN_MATCH, POST_MATCH, NEXT }

    @Volatile private var running = false
    private var job: Job? = null
    var status: String = "Остановлен"
        private set
    var onUpdate: (() -> Unit)? = null

    private val svc get() = BotAccessibilityService.instance

    fun start(scope: CoroutineScope) {
        if (running) return
        running = true
        BotBrain.reset()
        job = scope.launch(Dispatchers.Default) { loop() }
    }

    fun stop() {
        running = false; job?.cancel(); status = "Остановлен"
        DebugBus.scene = null; DebugBus.view?.postInvalidate(); onUpdate?.invoke()
    }

    private suspend fun loop() {
        var phase = Phase.NEXT
        var task: BrawlerTask? = null
        var matchStart = 0L

        while (running) {
            try {
                when (phase) {
                    Phase.NEXT -> {
                        task = QueueRepository.nextPending()
                        if (task == null) { status = "Очередь пуста — всё готово"; stop(); return }
                        status = "Выбираю ${task.brawler}"
                        phase = Phase.SELECT
                    }
                    Phase.SELECT -> {
                        if (selectBrawler(task!!.brawler)) phase = Phase.READ_TROPHIES
                        else { status = "Не нашёл ${task.brawler}, пропуск"; phase = Phase.NEXT }
                    }
                    Phase.READ_TROPHIES -> {
                        val t = readTrophies()
                        if (t != null) task!!.currentTrophies = t
                        QueueRepository.save()
                        if (t != null && t >= task!!.targetTrophies) {
                            status = "${task.brawler}: $t/${task.targetTrophies} — готово"
                            task.done = true; QueueRepository.save(); phase = Phase.NEXT
                        } else {
                            status = "${task.brawler}: $t/${task.targetTrophies} — играю"
                            phase = Phase.PRESS_PLAY
                        }
                    }
                    Phase.PRESS_PLAY -> {
                        svc?.tap(Config.x(Config.PLAY_BTN_X), Config.y(Config.PLAY_BTN_Y))
                        delay(2500)
                        matchStart = System.currentTimeMillis()
                        phase = Phase.IN_MATCH
                    }
                    Phase.IN_MATCH -> {
                        val over = tick()
                        if (over || System.currentTimeMillis() - matchStart > Config.maxMatchSeconds * 1000L)
                            phase = Phase.POST_MATCH
                    }
                    Phase.POST_MATCH -> {
                        svc?.tap(Config.x(Config.CONTINUE_X), Config.y(Config.CONTINUE_Y))
                        delay(2000)
                        phase = Phase.READ_TROPHIES
                    }
                    Phase.IDLE -> delay(200)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status = "Ошибка: ${e.message}"; delay(400) }
            onUpdate?.invoke()
            delay(Config.loopDelay)
        }
    }

    private suspend fun tick(): Boolean {
        val frame = ScreenCaptureService.latest() ?: return false

        if (Ocr.containsAny(Ocr.read(cropResultStrip(frame)),
                listOf("PLAY AGAIN", "CONTINUE", "REMATCH", "VICTORY", "DEFEAT")))
            return true

        val scene = Vision.analyze(frame)
        val d = BotBrain.decide(scene)
        DebugBus.push(scene, d, status)
        apply(d)
        return false
    }

    private suspend fun apply(d: Decision) {
        val s = svc ?: return

        if (d.moveX != 0f || d.moveY != 0f) {
            s.swipe(Config.x(Config.JOY_X), Config.y(Config.JOY_Y),
                Config.x(Config.JOY_X) + d.moveX * Config.x(0.13f),
                Config.y(Config.JOY_Y) + d.moveY * Config.x(0.13f), Config.moveMs)
        }
        if (d.hyper) s.tap(Config.x(Config.HYPER_X), Config.y(Config.HYPER_Y), Config.tapMs)
        d.aimSuper?.let { (ax, ay) ->
            val n = kotlin.math.hypot(ax, ay).coerceAtLeast(1f)
            s.swipe(Config.x(Config.SUPER_X), Config.y(Config.SUPER_Y),
                Config.x(Config.SUPER_X) + ax / n * Config.x(0.13f),
                Config.y(Config.SUPER_Y) + ay / n * Config.x(0.13f), 140L)
        }
        if (d.gadget) s.tap(Config.x(Config.GADGET_X), Config.y(Config.GADGET_Y), Config.tapMs)
        if (d.attack) s.tap(Config.x(Config.ATK_X), Config.y(Config.ATK_Y), Config.tapMs)
    }

    private suspend fun selectBrawler(name: String): Boolean {
        repeat(8) {
            val frame = ScreenCaptureService.latest() ?: return@repeat
            val words = Ocr.read(cropGrid(frame))
            val hit = Ocr.matchBrawler(words, name)
            if (hit != null) {
                svc?.tap(Config.x(Config.GRID_L) + hit.box.centerX(),
                    Config.y(Config.GRID_T) + hit.box.centerY() - Config.y(0.06f))
                delay(700)
                return true
            }
            val s = svc ?: return false
            s.swipe(Config.x(0.5f), Config.y(0.55f),
                Config.x(0.5f), Config.y(0.55f + Config.SCROLL_UP_DY), 400)
            delay(600)
        }
        return false
    }

    private fun cropGrid(f: Bitmap): Bitmap {
        val x = Config.x(Config.GRID_L).toInt().coerceIn(0, f.width - 1)
        val y = Config.y(Config.GRID_T).toInt().coerceIn(0, f.height - 1)
        val w = (Config.x(Config.GRID_R) - x).toInt().coerceIn(1, f.width - x)
        val h = (Config.y(Config.GRID_B) - y).toInt().coerceIn(1, f.height - y)
        return Bitmap.createBitmap(f, x, y, w, h)
    }

    private suspend fun readTrophies(): Int? {
        val frame = ScreenCaptureService.latest() ?: return null
        return Ocr.firstInt(Ocr.read(frame))
    }

    private fun cropResultStrip(f: Bitmap): Bitmap {
        val y = Config.y(Config.RESULT_STRIP_T).toInt().coerceIn(0, f.height - 1)
        val h = (Config.y(Config.RESULT_STRIP_B) - y).toInt().coerceIn(1, f.height - y)
        return Bitmap.createBitmap(f, 0, y, f.width, h)
    }
}