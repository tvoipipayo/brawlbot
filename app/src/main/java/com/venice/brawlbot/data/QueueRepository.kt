package com.venice.brawlbot.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object QueueRepository {
    private const val PREFS = "brawlbot"
    private const val KEY = "queue"
    private val gson = Gson()

    var tasks: MutableList<BrawlerTask> = mutableListOf()
        private set

    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
        load()
    }

    private fun load() {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return
        val type = object : TypeToken<MutableList<BrawlerTask>>() {}.type
        tasks = gson.fromJson(json, type) ?: mutableListOf()
    }

    fun save() {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, gson.toJson(tasks)).apply()
    }

    fun add(t: BrawlerTask) { tasks.add(t); save() }
    fun removeAt(i: Int) { if (i in tasks.indices) { tasks.removeAt(i); save() } }
    fun move(from: Int, to: Int) {
        if (from in tasks.indices && to in tasks.indices) {
            tasks.add(to, tasks.removeAt(from)); save()
        }
    }
    fun nextPending(): BrawlerTask? = tasks.firstOrNull { !it.done }
}