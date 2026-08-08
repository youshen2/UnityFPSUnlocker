package io.github.hexstr.UnityFPSUnlocker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogEntry
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogStatus
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogStore
import io.github.hexstr.UnityFPSUnlocker.logging.ModuleLogRepository
import io.github.hexstr.UnityFPSUnlocker.logging.ModuleLogSnapshot
import io.github.hexstr.UnityFPSUnlocker.ui.components.BackButton
import io.github.hexstr.UnityFPSUnlocker.ui.components.SettingsSection
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.DateFormat
import java.util.Date

@Composable
internal fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var snapshot by remember { mutableStateOf<ModuleLogSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching { ModuleLogRepository.readLogs(context) }
            .onSuccess { snapshot = it }
            .onFailure { error = it.localizedMessage ?: "读取 Hook 日志失败" }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Hook 日志",
                largeTitle = "Hook 日志",
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    IconButton(
                        onClick = { refreshKey++ },
                        enabled = !loading,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "刷新 Hook 日志",
                        )
                    }
                },
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
                SettingsSection(title = "状态") {
                    BasicComponent(
                        title = "本地记录",
                        summary = snapshot?.let { current ->
                            "已保存 ${current.entries.size} 条，最多保留 ${HookLogStore.MAX_ENTRIES} 条"
                        } ?: "正在读取",
                    )
                    snapshot?.let { current ->
                        BasicComponent(
                            title = "最近刷新",
                            summary = DateFormat.getDateTimeInstance().format(Date(current.capturedAt)),
                        )
                    }
                }
            }

            when {
                loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        BasicComponent(
                            title = "读取失败",
                            summary = error,
                            titleColor = BasicComponentDefaults.titleColor(
                                color = MiuixTheme.colorScheme.error,
                            ),
                        )
                    }
                }

                snapshot?.entries.isNullOrEmpty() -> item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        BasicComponent(
                            title = "暂无 Hook 记录",
                            summary = "请先完全结束并重新启动目标应用，然后返回此页刷新。",
                        )
                    }
                }

                else -> {
                    item {
                        SmallTitle(
                            text = "最近记录 · ${snapshot?.entries?.size ?: 0}",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(
                        items = snapshot?.entries.orEmpty(),
                    ) { entry ->
                        HookLogCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HookLogCard(entry: HookLogEntry) {
    val (statusLabel, statusColor) = when (entry.status) {
        HookLogStatus.Success -> "成功" to MiuixTheme.colorScheme.primary
        HookLogStatus.Failed -> "失败" to MiuixTheme.colorScheme.error
        HookLogStatus.Skipped -> "已跳过" to MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Card(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        BasicComponent(
            title = "$statusLabel · ${entry.packageName}",
            summary = listOfNotNull(
                DateFormat.getDateTimeInstance().format(Date(entry.timestamp)),
                formatHookMessage(entry.message),
            ).joinToString("\n"),
            titleColor = BasicComponentDefaults.titleColor(color = statusColor),
        )
    }
}

private fun formatHookMessage(message: String): String = when {
    message == HookLogContract.MESSAGE_APP_POLICY -> "已按应用策略跳过"
    message == HookLogContract.MESSAGE_NO_ENABLED_PARAMETERS -> "没有启用任何修改参数"
    message == HookLogContract.MESSAGE_UNITY_PLAYER_CLASS_NOT_FOUND ->
        "未找到 UnityPlayer，无法执行 Hook"
    message == HookLogContract.MESSAGE_NATIVE_INITIALIZATION -> "native 初始化失败"
    message == HookLogContract.MESSAGE_UNITY_ICALL_UNAVAILABLE -> "目标 Unity icall 不可用"
    message == HookLogContract.MESSAGE_LIBIL2CPP_NOT_FOUND -> "未找到 libil2cpp.so"
    message == HookLogContract.MESSAGE_DISPLAY_MODE_ACTIVITY_NOT_FOUND ->
        "显示模式 Hook 未取得 Activity"
    message == HookLogContract.MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND ->
        "未找到可用的 UnityPlayer 构造函数"
    message == HookLogContract.MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION ->
        "显示模式 Hook 初始化失败"
    message.startsWith(HookLogContract.FIELD_DISPLAY_MODE_ID) ->
        "显示模式设置成功 · ${message.substringAfter(HookLogContract.FIELD_DISPLAY_MODE_ID)}"
    else -> message
        .replace("fps=-1", "FPS 不修改")
        .replace("resolution_scale=-1.000", "分辨率不修改")
        .replace(HookLogContract.FIELD_FPS, "FPS=")
        .replace(HookLogContract.FIELD_RESOLUTION_SCALE, "分辨率缩放=")
}
