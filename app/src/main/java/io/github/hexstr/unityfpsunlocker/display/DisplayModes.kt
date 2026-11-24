package io.github.hexstr.UnityFPSUnlocker.display

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import io.github.hexstr.UnityFPSUnlocker.config.ModuleConfig
import java.util.Locale

data class DisplayModeOption(
    val id: Int,
    val label: String,
)

fun loadDisplayModes(activity: Activity): List<DisplayModeOption> {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
    } else {
        @Suppress("DEPRECATION")
        (activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
    }

    val modes = display?.supportedModes.orEmpty()
        .map { mode ->
            DisplayModeOption(
                id = mode.modeId,
                label = "${mode.physicalWidth} × ${mode.physicalHeight} · " +
                    "${"%.2f".format(Locale.US, mode.refreshRate)} Hz",
            )
        }
        .distinctBy(DisplayModeOption::id)

    return listOf(
        DisplayModeOption(
            ModuleConfig.NO_DISPLAY_MODE_ID,
            "不修改（由系统管理）",
        ),
    ) + modes
}
