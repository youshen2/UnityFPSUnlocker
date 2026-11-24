package io.github.hexstr.UnityFPSUnlocker.config

import android.content.Context
import android.content.SharedPreferences

/**
 * 基于 [SharedPreferences] 的配置访问层。
 *
 * 同一套代码同时服务于：
 * - 模块界面：XposedService 提供的可写远程首选项；
 * - 被 Hook 进程：[io.github.libxposed.api.XposedModule.getRemotePreferences] 返回的只读视图。
 */
class ConfigStore(val prefs: SharedPreferences) {

    /* ---------------------------------- 全局 ---------------------------------- */

    fun readGlobal(): ModuleConfig = read(null)

    fun writeGlobal(config: ModuleConfig) {
        write(null, config)
    }

    fun onlyManagedApps(): Boolean = prefs.getBoolean(KEY_ONLY_MANAGED, true)

    fun setOnlyManagedApps(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONLY_MANAGED, value).apply()
    }

    /* ---------------------------------- 应用 ---------------------------------- */

    fun managedApps(): Set<String> =
        prefs.getStringSet(KEY_MANAGED_APPS, emptySet())?.toSet().orEmpty()

    fun isManaged(packageName: String): Boolean = packageName in managedApps()

    fun appEnabled(packageName: String): Boolean =
        prefs.getBoolean(appKey(packageName, KEY_APP_ENABLED), true)

    fun appUsesGlobal(packageName: String): Boolean =
        prefs.getBoolean(appKey(packageName, KEY_APP_USE_GLOBAL), false)

    fun readApp(packageName: String): ModuleConfig = read(packageName)

    fun writeApp(packageName: String, config: ModuleConfig) {
        write(packageName, config)
    }

    /** 加入应用管理器，并默认使用全局配置。 */
    fun manage(packageName: String, enabled: Boolean = true) {
        val packages = managedApps().toMutableSet().apply { add(packageName) }
        prefs.edit()
            .putStringSet(KEY_MANAGED_APPS, packages)
            .putBoolean(appKey(packageName, KEY_APP_ENABLED), enabled)
            .putBoolean(appKey(packageName, KEY_APP_USE_GLOBAL), false)
            .apply()
    }

    /** 仅切换启用状态，保留已填写的独立参数。 */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        val packages = managedApps().toMutableSet().apply { add(packageName) }
        prefs.edit()
            .putStringSet(KEY_MANAGED_APPS, packages)
            .putBoolean(appKey(packageName, KEY_APP_ENABLED), enabled)
            .apply()
    }

    fun setAppUsesGlobal(packageName: String, useGlobal: Boolean) {
        prefs.edit()
            .putBoolean(appKey(packageName, KEY_APP_USE_GLOBAL), useGlobal)
            .apply()
    }

    fun removeApp(packageName: String) {
        val packages = managedApps().toMutableSet().apply { remove(packageName) }
        prefs.edit()
            .putStringSet(KEY_MANAGED_APPS, packages)
            .putBoolean(appKey(packageName, KEY_APP_ENABLED), false)
            .remove(appKey(packageName, KEY_APP_USE_GLOBAL))
            .apply()
    }

    /* --------------------------------- 运行时 --------------------------------- */

    /**
     * 计算目标进程最终生效的配置。
     *
     * 返回 null 表示该进程应被完全跳过：已显式禁用，或开启了“仅处理受管理应用”
     * 而该包未加入应用管理器，或没有任何启用的参数。
     */
    fun resolveForPackage(packageName: String): ModuleConfig? {
        val managed = isManaged(packageName)
        if (managed && !appEnabled(packageName)) return null
        if (!managed && onlyManagedApps()) return null

        return if (managed && !appUsesGlobal(packageName)) {
            readApp(packageName)
        } else {
            readGlobal()
        }
    }

    /* --------------------------------- 读写实现 --------------------------------- */

    private fun read(packageName: String?): ModuleConfig {
        fun key(name: String): String =
            if (packageName == null) name else appKey(packageName, name)

        return ModuleConfig(
            fps = prefs.getInt(key(KEY_FPS), ModuleConfig.DEFAULT_FPS),
            fpsEnabled = prefs.getBoolean(key(KEY_FPS_ENABLED), ModuleConfig.DEFAULT_FPS_ENABLED),
            delay = prefs.getInt(key(KEY_DELAY), ModuleConfig.DEFAULT_DELAY),
            delayEnabled = prefs.getBoolean(
                key(KEY_DELAY_ENABLED),
                ModuleConfig.DEFAULT_DELAY_ENABLED,
            ),
            scale = prefs.getFloat(key(KEY_SCALE), ModuleConfig.DEFAULT_SCALE),
            scaleEnabled = prefs.getBoolean(
                key(KEY_SCALE_ENABLED),
                ModuleConfig.DEFAULT_SCALE_ENABLED,
            ),
            displayModeId = prefs.getInt(
                key(KEY_DISPLAY_MODE_ID),
                ModuleConfig.DEFAULT_DISPLAY_MODE_ID,
            ),
            displayModeEnabled = prefs.getBoolean(
                key(KEY_DISPLAY_MODE_ENABLED),
                ModuleConfig.DEFAULT_DISPLAY_MODE_ENABLED,
            ),
            modifyOpcode = prefs.getBoolean(
                key(KEY_MODIFY_OPCODE),
                ModuleConfig.DEFAULT_MODIFY_OPCODE,
            ),
        )
    }

    private fun write(packageName: String?, config: ModuleConfig) {
        fun key(name: String): String =
            if (packageName == null) name else appKey(packageName, name)

        prefs.edit()
            .putInt(key(KEY_FPS), config.fps)
            .putBoolean(key(KEY_FPS_ENABLED), config.fpsEnabled)
            .putInt(key(KEY_DELAY), config.delay)
            .putBoolean(key(KEY_DELAY_ENABLED), config.delayEnabled)
            .putFloat(key(KEY_SCALE), config.scale)
            .putBoolean(key(KEY_SCALE_ENABLED), config.scaleEnabled)
            .putInt(key(KEY_DISPLAY_MODE_ID), config.displayModeId)
            .putBoolean(key(KEY_DISPLAY_MODE_ENABLED), config.displayModeEnabled)
            .putBoolean(key(KEY_MODIFY_OPCODE), config.modifyOpcode)
            .apply()
    }

    companion object {
        fun localPreferences(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_GROUP, Context.MODE_PRIVATE)

        /**
         * 首次绑定框架时把本地预览配置迁移到远程首选项，避免用户之前保存的设置丢失。
         */
        fun migrateLocalToRemote(local: SharedPreferences, remote: SharedPreferences) {
            if (remote.all.isNotEmpty()) return
            val values = local.all
            if (values.isEmpty()) return
            remote.edit().apply {
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            putStringSet(key, value as Set<String>)
                        }
                    }
                }
            }.apply()
        }
    }
}
