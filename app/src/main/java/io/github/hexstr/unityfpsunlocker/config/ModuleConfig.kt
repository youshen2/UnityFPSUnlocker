package io.github.hexstr.UnityFPSUnlocker.config

/**
 * 一套帧率 / 延迟 / 分辨率 / 显示模式配置。
 *
 * 每项都有独立开关；关闭时向 native 侧传递“不修改”哨兵值，而不是把数值清零。
 */
data class ModuleConfig(
    val fps: Int = DEFAULT_FPS,
    val fpsEnabled: Boolean = DEFAULT_FPS_ENABLED,
    val delay: Int = DEFAULT_DELAY,
    val delayEnabled: Boolean = DEFAULT_DELAY_ENABLED,
    val scale: Float = DEFAULT_SCALE,
    val scaleEnabled: Boolean = DEFAULT_SCALE_ENABLED,
    val displayModeId: Int = DEFAULT_DISPLAY_MODE_ID,
    val displayModeEnabled: Boolean = DEFAULT_DISPLAY_MODE_ENABLED,
    val modifyOpcode: Boolean = DEFAULT_MODIFY_OPCODE,
) {
    val effectiveFps: Int get() = if (fpsEnabled) fps else NO_FPS
    val effectiveDelay: Int get() = if (delayEnabled) delay else 0
    val effectiveScale: Float get() = if (scaleEnabled) scale else NO_SCALE
    val effectiveDisplayModeId: Int
        get() = if (displayModeEnabled) displayModeId else NO_DISPLAY_MODE_ID

    val hasNativeParams: Boolean get() = effectiveFps > 0 || effectiveScale > 0f
    val hasAnyParam: Boolean get() = hasNativeParams || effectiveDisplayModeId > 0

    companion object {
        const val NO_FPS = -1
        const val NO_SCALE = -1f
        const val NO_DISPLAY_MODE_ID = -1

        const val DEFAULT_FPS = 90
        const val DEFAULT_FPS_ENABLED = false
        const val DEFAULT_DELAY = 5
        const val DEFAULT_DELAY_ENABLED = false
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_SCALE_ENABLED = false
        const val DEFAULT_DISPLAY_MODE_ID = NO_DISPLAY_MODE_ID
        const val DEFAULT_DISPLAY_MODE_ENABLED = false
        const val DEFAULT_MODIFY_OPCODE = true

        const val MIN_FPS = 1
        const val MAX_FPS = 1000
        const val MIN_DELAY = 1
        const val MAX_DELAY = 120
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 4f
    }
}
