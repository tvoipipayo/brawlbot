package com.venice.brawlbot.vision

data class Blip(val x: Float, val y: Float, val size: Float)

data class Scene(
    val enemies: List<Blip> = emptyList(),
    val selfHp: Float = 1f,
    val ammo: Int = 3,
    val superReady: Boolean = false,
    val hyperReady: Boolean = false,
    val gadgetReady: Boolean = false,
    val visible: Boolean = false
)

data class Decision(
    var moveX: Float = 0f,
    var moveY: Float = 0f,
    var attack: Boolean = false,
    var aimSuper: Pair<Float, Float>? = null,
    var gadget: Boolean = false,
    var hyper: Boolean = false
)