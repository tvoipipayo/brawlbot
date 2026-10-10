package com.venice.brawlbot

object Config {
    var screenW = 0
    var screenH = 0
    fun x(f: Float) = screenW * f
    fun y(f: Float) = screenH * f

    const val GRID_L = 0.03f; const val GRID_R = 0.97f
    const val GRID_T = 0.18f; const val GRID_B = 0.62f
    const val PLAY_BTN_X = 0.50f; const val PLAY_BTN_Y = 0.87f
    const val SCROLL_UP_DY = -0.35f

    const val JOY_X = 0.14f; const val JOY_Y = 0.80f
    const val ATK_X = 0.87f; const val ATK_Y = 0.82f
    const val SUPER_X = 0.72f; const val SUPER_Y = 0.85f
    const val GADGET_X = 0.875f; const val GADGET_Y = 0.665f
    const val HYPER_X = 0.70f; const val HYPER_Y = 0.70f

    const val AMMO_L = 0.78f; const val AMMO_R = 0.97f
    const val AMMO_T = 0.665f; const val AMMO_B = 0.715f
    const val SUPER_L = 0.665f; const val SUPER_R = 0.775f
    const val SUPER_T = 0.795f; const val SUPER_B = 0.905f
    const val HYPER_L = 0.65f; const val HYPER_R = 0.75f
    const val HYPER_T = 0.645f; const val HYPER_B = 0.735f
    const val GADGET_L = 0.83f; const val GADGET_R = 0.93f
    const val GADGET_T = 0.615f; const val GADGET_B = 0.715f

    const val BATTLE_T = 0.16f; const val BATTLE_B = 0.70f
    const val RESULT_STRIP_T = 0.72f; const val RESULT_STRIP_B = 0.95f
    const val CONTINUE_X = 0.50f; const val CONTINUE_Y = 0.86f

    var maxHealthBarPx = 68f
    var enemyMinArea = 45.0
    var enemyMaxArea = 6000.0

    var tapMs = 40L
    var moveMs = 120L
    var loopDelay = 40L
    var maxMatchSeconds = 200
}