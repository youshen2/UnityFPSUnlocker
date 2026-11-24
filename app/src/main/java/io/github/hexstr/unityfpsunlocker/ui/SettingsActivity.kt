package io.github.hexstr.UnityFPSUnlocker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnityFpsTheme {
                SettingsNavigationHost {
                    SettingsApp()
                }
            }
        }
    }
}

/**
 * MIUIX 0.9 的 OverlayDialog 会查询 NavigationEventDispatcherOwner，
 * 必须由 Activity 侧提供，否则打开数字/显示模式弹窗时会崩溃。
 */
@Composable
private fun SettingsNavigationHost(content: @Composable () -> Unit) {
    val activityOwner = LocalNavigationEventDispatcherOwner.current
    val dispatcherOwner = rememberNavigationEventDispatcherOwner(parent = activityOwner)
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides dispatcherOwner,
        content = content,
    )
}

@Composable
private fun UnityFpsTheme(content: @Composable () -> Unit) {
    val mode = if (isSystemInDarkTheme()) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember(mode) { ThemeController(mode) }
    MiuixTheme(controller = controller, content = content)
}
