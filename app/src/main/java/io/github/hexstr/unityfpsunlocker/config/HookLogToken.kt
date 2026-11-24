package io.github.hexstr.UnityFPSUnlocker.config

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Hook 进程向模块进程回传日志时使用的共享令牌。
 *
 * 模块进程始终在本地保存一份副本，接收广播时无需等待 XposedService 绑定；
 * 框架可用时再同步到远程首选项供被 Hook 进程读取。
 */
object HookLogToken {

    fun ensure(context: Context, remote: SharedPreferences? = null) {
        val local = ConfigStore.localPreferences(context)
        var token = local.getString(KEY_HOOK_LOG_TOKEN, null)
        if (token.isNullOrBlank()) {
            token = UUID.randomUUID().toString()
            local.edit().putString(KEY_HOOK_LOG_TOKEN, token).apply()
        }
        if (remote != null) {
            runCatching {
                if (remote.getString(KEY_HOOK_LOG_TOKEN, null) != token) {
                    remote.edit().putString(KEY_HOOK_LOG_TOKEN, token).apply()
                }
            }
        }
    }

    fun local(context: Context): String? =
        ConfigStore.localPreferences(context).getString(KEY_HOOK_LOG_TOKEN, null)
}
