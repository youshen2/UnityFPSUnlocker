package io.github.hexstr.UnityFPSUnlocker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.hexstr.UnityFPSUnlocker.config.ModuleConfig
import io.github.hexstr.UnityFPSUnlocker.display.DisplayModeOption

@Composable
internal fun ModuleConfigEditor(
    sectionTitle: String,
    config: ModuleConfig,
    displayModes: List<DisplayModeOption>,
    onConfigChange: (ModuleConfig) -> Unit,
) {
    var editingField by remember { mutableStateOf<ConfigField?>(null) }
    var showDisplayModes by remember { mutableStateOf(false) }

    SettingsSection(title = sectionTitle) {
        OptionalValueSetting(
            title = "目标 FPS",
            summary = fpsSummary(config.fps, config.fpsEnabled),
            enabled = config.fpsEnabled,
            onEnabledChange = { enabled ->
                onConfigChange(config.copy(fpsEnabled = enabled))
            },
            onClick = { editingField = ConfigField.Fps },
        )
        OptionalValueSetting(
            title = "注入延迟",
            summary = delaySummary(config.delay, config.delayEnabled),
            enabled = config.delayEnabled,
            onEnabledChange = { enabled ->
                onConfigChange(config.copy(delayEnabled = enabled))
            },
            onClick = { editingField = ConfigField.Delay },
        )
        OptionalValueSetting(
            title = "渲染缩放",
            summary = scaleSummary(config.scale, config.scaleEnabled),
            enabled = config.scaleEnabled,
            onEnabledChange = { enabled ->
                onConfigChange(config.copy(scaleEnabled = enabled))
            },
            onClick = { editingField = ConfigField.Scale },
        )
        OptionalValueSetting(
            title = "显示模式",
            summary = displayModeLabel(
                config.displayModeId,
                displayModes,
                config.displayModeEnabled,
            ),
            enabled = config.displayModeEnabled,
            onEnabledChange = { enabled ->
                if (enabled && config.displayModeId <= 0) {
                    showDisplayModes = true
                } else {
                    onConfigChange(config.copy(displayModeEnabled = enabled))
                }
            },
            onClick = { showDisplayModes = true },
        )
        if (config.fpsEnabled) {
            ToggleSetting(
                title = "锁定帧率设置",
                summary = "把 set_targetFrameRate 首指令改为返回，避免游戏启动后覆盖目标帧率",
                checked = config.modifyOpcode,
                onCheckedChange = { onConfigChange(config.copy(modifyOpcode = it)) },
            )
        }
    }

    editingField?.let { field ->
        NumberEditorDialog(
            spec = numberFieldSpec(field, config),
            onDismiss = { editingField = null },
            onConfirm = { rawValue ->
                val nextConfig = when (field) {
                    ConfigField.Delay -> config.copy(delay = rawValue.toInt())
                    ConfigField.Fps -> config.copy(fps = rawValue.toInt())
                    ConfigField.Scale -> config.copy(scale = rawValue.toFloat())
                }
                onConfigChange(nextConfig)
                editingField = null
            },
        )
    }

    DisplayModeDialog(
        show = showDisplayModes,
        selectedId = config.displayModeId,
        displayModes = displayModes,
        onDismiss = { showDisplayModes = false },
        onSelect = { id ->
            onConfigChange(
                config.copy(
                    displayModeId = id,
                    displayModeEnabled = true,
                ),
            )
            showDisplayModes = false
        },
    )
}
