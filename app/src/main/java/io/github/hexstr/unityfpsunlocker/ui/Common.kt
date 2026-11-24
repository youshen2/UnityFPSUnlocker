package io.github.hexstr.UnityFPSUnlocker.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.hexstr.UnityFPSUnlocker.App
import io.github.hexstr.UnityFPSUnlocker.config.ConfigStore
import io.github.hexstr.UnityFPSUnlocker.config.PREFS_GROUP

/**
 * 设置界面使用的配置存储：框架绑定时读取 XposedService 提供的可写远程首选项，
 * 否则退化为应用私有文件（便于在未激活框架时预览和保存设置）。
 */
@Composable
fun rememberConfigStore(): ConfigStore {
    val context = LocalContext.current
    val service = App.serviceState.value
    val prefs: SharedPreferences = remember(context, service) {
        val remote = service?.let {
            runCatching { it.getRemotePreferences(PREFS_GROUP) }.getOrNull()
        }
        remote ?: ConfigStore.localPreferences(context)
    }
    return remember(prefs) { ConfigStore(prefs) }
}
