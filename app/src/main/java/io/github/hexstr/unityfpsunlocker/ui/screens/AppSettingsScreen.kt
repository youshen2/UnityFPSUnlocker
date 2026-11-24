package io.github.hexstr.UnityFPSUnlocker.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.config.AppPolicy
import io.github.hexstr.UnityFPSUnlocker.config.ModuleConfig
import io.github.hexstr.UnityFPSUnlocker.config.ModulePreferences
import io.github.hexstr.UnityFPSUnlocker.apps.InstalledApp
import io.github.hexstr.UnityFPSUnlocker.display.DisplayModeOption
import io.github.hexstr.UnityFPSUnlocker.ui.components.AppIcon
import io.github.hexstr.UnityFPSUnlocker.ui.components.BackButton
import io.github.hexstr.UnityFPSUnlocker.ui.components.ModuleConfigEditor
import io.github.hexstr.UnityFPSUnlocker.ui.components.SettingsSection
import io.github.hexstr.UnityFPSUnlocker.ui.components.ToggleSetting
import io.github.hexstr.UnityFPSUnlocker.ui.ScopeManager
import io.github.hexstr.UnityFPSUnlocker.ui.components.delaySummary
import io.github.hexstr.UnityFPSUnlocker.ui.components.fpsSummary
import io.github.hexstr.UnityFPSUnlocker.ui.components.scaleSummary
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun AppSettingsScreen(
    app: InstalledApp,
    globalConfig: ModuleConfig,
    displayModes: List<DisplayModeOption>,
    repository: ModulePreferences,
    onBack: () -> Unit,
    onPolicyChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var policy by remember(app.packageName, repository) {
        mutableStateOf(repository.readAppPolicy(app.packageName))
    }
    val shownConfig = if (policy.useGlobal) globalConfig else policy.config

    fun save(next: AppPolicy) {
        val managedPolicy = next.copy(managed = true)
        policy = managedPolicy
        repository.writeAppPolicy(managedPolicy)
        onPolicyChanged()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = app.label,
                largeTitle = app.label,
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton(onClick = onBack) },
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
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    BasicComponent(
                        title = app.label,
                        summary = app.packageName,
                        startAction = { AppIcon(app = app, size = 52) },
                    )
                }
            }

            item {
                SettingsSection(title = "应用策略") {
                    ToggleSetting(
                        title = "对该应用生效",
                        summary = if (policy.managed && policy.enabled) {
                            "模块会按下方配置处理该应用"
                        } else {
                            "关闭后不会在该应用中执行 Hook"
                        },
                        checked = policy.managed && policy.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !policy.managed) {
                                save(
                                    policy.copy(
                                        managed = true,
                                        enabled = true,
                                        useGlobal = false,
                                        config = globalConfig,
                                    ),
                                )
                            } else if (policy.managed) {
                                save(policy.copy(enabled = enabled))
                            }
                            if (enabled) {
                                ScopeManager.request(context, app.packageName)
                            } else {
                                val disabledPolicy = policy.copy(enabled = false)
                                save(disabledPolicy)
                                ScopeManager.remove(context, app.packageName) { inScope ->
                                    if (inScope == false) {
                                        repository.writeAppPolicy(
                                            disabledPolicy.copy(managed = false),
                                        )
                                        policy = repository.readAppPolicy(app.packageName)
                                        onPolicyChanged()
                                    }
                                }
                            }
                        },
                    )
                    if (policy.managed) {
                        ToggleSetting(
                            title = "使用全局默认值",
                            summary = if (policy.useGlobal) "全局修改会自动作用于此应用" else "此应用使用下方独立参数",
                            checked = policy.useGlobal,
                            onCheckedChange = { useGlobal ->
                                save(policy.copy(useGlobal = useGlobal))
                            },
                        )
                    }
                }
            }

            if (policy.managed && !policy.useGlobal) {
                item {
                    ModuleConfigEditor(
                        sectionTitle = "独立参数",
                        config = shownConfig,
                        displayModes = displayModes,
                        onConfigChange = { nextConfig ->
                            save(policy.copy(config = nextConfig))
                        },
                    )
                }
            }

            if (policy.managed) {
                item {
                    SettingsSection(title = "当前生效配置") {
                        BasicComponent(
                            title = if (policy.enabled) "模块已启用" else "模块已禁用",
                            summary = if (!policy.enabled) {
                                "目标进程将被直接跳过"
                            } else if (policy.useGlobal) {
                                "使用全局值：${fpsSummary(globalConfig.fps, globalConfig.fpsEnabled)} · 分辨率 ${scaleSummary(globalConfig.scale, globalConfig.scaleEnabled)} · 延迟 ${delaySummary(globalConfig.delay, globalConfig.delayEnabled)}"
                            } else {
                                "独立值：${fpsSummary(shownConfig.fps, shownConfig.fpsEnabled)} · 分辨率 ${scaleSummary(shownConfig.scale, shownConfig.scaleEnabled)} · 延迟 ${delaySummary(shownConfig.delay, shownConfig.delayEnabled)}"
                            },
                        )
                    }
                }
            }
        }

    }
}
