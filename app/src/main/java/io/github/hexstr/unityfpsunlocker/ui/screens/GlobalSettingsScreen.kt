package io.github.hexstr.UnityFPSUnlocker.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.ModuleConfig
import io.github.hexstr.UnityFPSUnlocker.display.DisplayModeOption
import io.github.hexstr.UnityFPSUnlocker.ui.components.ModuleConfigEditor
import io.github.hexstr.UnityFPSUnlocker.ui.components.SettingsSection
import io.github.hexstr.UnityFPSUnlocker.ui.components.ToggleSetting
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun GlobalSettingsScreen(
    config: ModuleConfig,
    onlyManagedApps: Boolean,
    managedCount: Int,
    displayModes: List<DisplayModeOption>,
    onConfigChange: (ModuleConfig) -> Unit,
    onOnlyManagedAppsChange: (Boolean) -> Unit,
    onOpenApps: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Unity FPS Unlocker",
                largeTitle = "Unity FPS Unlocker",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp,
            ),
        ) {
            item {
                SettingsSection(title = "按应用管理") {
                    SuperArrow(
                        title = "应用管理器",
                        summary = if (managedCount == 0) {
                            "选择应用并设置独立的帧率、延迟和显示模式"
                        } else {
                            "已管理 $managedCount 个应用"
                        },
                        onClick = onOpenApps,
                    )
                    ToggleSetting(
                        title = "仅处理受管理应用",
                        summary = "开启后，未加入应用管理器的进程会被直接跳过",
                        checked = onlyManagedApps,
                        onCheckedChange = onOnlyManagedAppsChange,
                    )
                }
            }

            item {
                ModuleConfigEditor(
                    sectionTitle = "全局默认值",
                    config = config,
                    displayModes = displayModes,
                    onConfigChange = onConfigChange,
                )
            }

            item {
                SettingsSection(title = "其他") {
                    SuperArrow(
                        title = "Hook 日志",
                        summary = "查看各应用最近的 Hook 成功、失败和跳过记录",
                        onClick = onOpenLogs,
                    )
                    SuperArrow(
                        title = "关于",
                        summary = "版本、作者与项目来源",
                        onClick = onOpenAbout,
                    )
                }
            }
        }

    }
}
