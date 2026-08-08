package io.github.hexstr.UnityFPSUnlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.hexstr.UnityFPSUnlocker.ui.SettingsApp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnityFpsTheme {
                SettingsApp()
            }
        }
    }
}

@Composable
private fun UnityFpsTheme(content: @Composable () -> Unit) {
    val mode = if (isSystemInDarkTheme()) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember(mode) { ThemeController(mode) }
    MiuixTheme(controller = controller, content = content)
}
