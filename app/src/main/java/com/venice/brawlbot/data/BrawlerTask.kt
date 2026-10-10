package com.venice.brawlbot.data

data class BrawlerTask(
    val brawler: String,
    val targetTrophies: Int,
    var currentTrophies: Int = -1,
    var done: Boolean = false
)