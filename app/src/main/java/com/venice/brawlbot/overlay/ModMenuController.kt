package com.venice.brawlbot.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.*
import com.venice.brawlbot.R
import com.venice.brawlbot.data.BrawlerCatalog
import com.venice.brawlbot.data.BrawlerTask
import com.venice.brawlbot.data.QueueRepository
import com.venice.brawlbot.engine.BotEngine
import kotlinx.coroutines.MainScope

class ModMenuController(
    private val ctx: Context,
    private val root: View,
    private val wm: WindowManager,
    private val lp: WindowManager.LayoutParams
) {
    private val scope = MainScope()
    private lateinit var tabBrawlers: View
    private lateinit var tabQueue: View
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    @SuppressLint("ClickableViewAccessibility")
    fun bind() {
        tabBrawlers = root.findViewById(R.id.tabBrawlers)
        tabQueue = root.findViewById(R.id.tabQueue)
        list = root.findViewById(R.id.taskList)
        status = root.findViewById(R.id.status)

        root.findViewById<Button>(R.id.btnTabBrawlers).setOnClickListener { switch(0) }
        root.findViewById<Button>(R.id.btnTabQueue).setOnClickListener { switch(1) }
        root.findViewById<Button>(R.id.btnStart).setOnClickListener { BotEngine.start(scope) }
        root.findViewById<Button>(R.id.btnStop).setOnClickListener { BotEngine.stop() }
        root.findViewById<Button>(R.id.btnDebug)
            .setOnClickListener { OverlayService.instance?.toggleDebug() }

        val spinner = root.findViewById<Spinner>(R.id.spinnerBrawler)
        spinner.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
            BrawlerCatalog.ALL)

        val target = root.findViewById<EditText>(R.id.editTarget)
        root.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = spinner.selectedItem?.toString() ?: return@setOnClickListener
            val to = target.text.toString().toIntOrNull() ?: 500
            QueueRepository.add(BrawlerTask(name, to))
            renderList()
        }

        BotEngine.onUpdate = { root.post { status.text = BotEngine.status; renderList() } }

        root.findViewById<View>(R.id.header).setOnTouchListener(dragListener())
        switch(0)
        renderList()
    }

    private fun switch(i: Int) {
        tabBrawlers.visibility = if (i == 0) View.VISIBLE else View.GONE
        tabQueue.visibility = if (i == 1) View.VISIBLE else View.GONE
    }

    private fun renderList() {
        list.removeAllViews()
        QueueRepository.tasks.forEachIndexed { idx, t ->
            val row = LayoutInflater.from(ctx).inflate(R.layout.item_task, list, false)
            row.findViewById<TextView>(R.id.tvName).text = t.brawler
            row.findViewById<TextView>(R.id.tvCurrent).text =
                "сейчас: " + if (t.currentTrophies >= 0) t.currentTrophies else "—"
            val to = row.findViewById<EditText>(R.id.tvTarget)
            to.setText(t.targetTrophies.toString())
            to.setOnFocusChangeListener { _, has ->
                if (!has) {
                    t.targetTrophies = to.text.toString().toIntOrNull() ?: t.targetTrophies
                    QueueRepository.save()
                }
            }
            row.findViewById<Button>(R.id.btnUp).setOnClickListener {
                QueueRepository.move(idx, (idx - 1).coerceAtLeast(0)); renderList()
            }
            row.findViewById<Button>(R.id.btnDown).setOnClickListener {
                QueueRepository.move(idx, (idx + 1).coerceAtMost(QueueRepository.tasks.size - 1))
                renderList()
            }
            row.findViewById<Button>(R.id.btnDel).setOnClickListener {
                QueueRepository.removeAt(idx); renderList()
            }
            if (t.done) row.alpha = 0.5f
            list.addView(row)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun dragListener() = View.OnTouchListener { _, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_MOVE -> {
                lp.x = (e.rawX - 80).toInt(); lp.y = (e.rawY - 60).toInt()
                wm.updateViewLayout(root, lp); true
            }
            else -> false
        }
    }
}