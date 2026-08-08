package io.github.hexstr.UnityFPSUnlocker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract
import java.util.UUID

data class ModuleConfig(
    val delay: Int = DEFAULT_DELAY,
    val delayEnabled: Boolean = DEFAULT_DELAY_ENABLED,
    val fps: Int = DEFAULT_FPS,
    val fpsEnabled: Boolean = DEFAULT_FPS_ENABLED,
    val scale: Float = DEFAULT_SCALE,
    val scaleEnabled: Boolean = DEFAULT_SCALE_ENABLED,
    val displayModeId: Int = DEFAULT_DISPLAY_MODE_ID,
    val displayModeEnabled: Boolean = DEFAULT_DISPLAY_MODE_ENABLED,
    val modOpcode: Boolean = DEFAULT_MOD_OPCODE,
) {
    companion object {
        const val NO_CHANGE_FPS = -1
        const val NO_CHANGE_SCALE = -1f
        const val NO_CHANGE_DISPLAY_MODE_ID = -1

        const val DEFAULT_DELAY = 5
        const val DEFAULT_DELAY_ENABLED = true
        const val DEFAULT_FPS = 90
        const val DEFAULT_FPS_ENABLED = true
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_SCALE_ENABLED = false
        const val DEFAULT_DISPLAY_MODE_ID = NO_CHANGE_DISPLAY_MODE_ID
        const val DEFAULT_DISPLAY_MODE_ENABLED = false
        const val DEFAULT_MOD_OPCODE = true
    }
}

data class AppPolicy(
    val packageName: String,
    val managed: Boolean,
    val enabled: Boolean,
    val useGlobal: Boolean,
    val config: ModuleConfig,
)

@SuppressLint("ApplySharedPref")
class ModulePreferences(context: Context) {
    companion object {
        const val PREFS_NAME = PreferenceContract.PREFS_NAME
        const val KEY_MANAGED_PACKAGES = PreferenceContract.KEY_MANAGED_PACKAGES
        const val KEY_ONLY_MANAGED_APPS = PreferenceContract.KEY_ONLY_MANAGED_APPS
        const val KEY_HOOK_LOG_TOKEN = PreferenceContract.KEY_HOOK_LOG_TOKEN

        private const val TAG = "UnityFPSUnlocker"
        private val CONFIG_KEYS = listOf(
            PreferenceContract.KEY_DELAY,
            PreferenceContract.KEY_FPS,
            PreferenceContract.KEY_SCALE,
            PreferenceContract.KEY_DISPLAY_MODE_ID,
            PreferenceContract.KEY_MOD_OPCODE,
        )
        fun perAppKey(packageName: String, key: String): String =
            PreferenceContract.perAppKey(packageName, key)
    }

    private val preferences: SharedPreferences = openPreferences(context.applicationContext)

    init {
        ensureHookLogToken()
        migrateLegacyPerAppEntries()
    }

    fun readGlobal(): ModuleConfig = readConfig(
        keyFor = { key -> key },
        fallback = ModuleConfig(),
    )

    fun writeGlobal(config: ModuleConfig) {
        writeConfig(
            editor = preferences.edit(),
            keyFor = { key -> key },
            config = config,
        ).commit()
    }

    fun onlyManagedApps(): Boolean =
        preferences.getBoolean(KEY_ONLY_MANAGED_APPS, false)

    fun setOnlyManagedApps(value: Boolean) {
        preferences.edit().putBoolean(KEY_ONLY_MANAGED_APPS, value).commit()
    }

    fun managedPackages(): Set<String> =
        preferences.getStringSet(KEY_MANAGED_PACKAGES, emptySet())?.toSet().orEmpty()

    fun readAppPolicy(packageName: String): AppPolicy {
        val global = readGlobal()
        return readAppPolicy(packageName, global, managedPackages())
    }

    fun readAppPolicies(packageNames: Iterable<String>): Map<String, AppPolicy> {
        val global = readGlobal()
        val managedPackages = managedPackages()
        return packageNames.associateWith { packageName ->
            readAppPolicy(packageName, global, managedPackages)
        }
    }

    fun enabledManagedCount(): Int = managedPackages().count { packageName ->
        preferences.getBoolean(
            perAppKey(packageName, PreferenceContract.KEY_ENABLED),
            true,
        )
    }

    private fun readAppPolicy(
        packageName: String,
        global: ModuleConfig,
        managedPackages: Set<String>,
    ): AppPolicy {
        val managed = packageName in managedPackages
        return AppPolicy(
            packageName = packageName,
            managed = managed,
            enabled = preferences.getBoolean(
                perAppKey(packageName, PreferenceContract.KEY_ENABLED),
                true,
            ),
            useGlobal = preferences.getBoolean(
                perAppKey(packageName, PreferenceContract.KEY_USE_GLOBAL),
                !managed,
            ),
            config = readConfig(
                keyFor = { key -> perAppKey(packageName, key) },
                fallback = global,
            ),
        )
    }

    fun writeAppPolicy(policy: AppPolicy) {
        val packages = managedPackages().toMutableSet().apply { add(policy.packageName) }
        val editor = preferences.edit()
            .putStringSet(KEY_MANAGED_PACKAGES, packages)
            .putBoolean(
                perAppKey(policy.packageName, PreferenceContract.KEY_ENABLED),
                policy.enabled,
            )
            .putBoolean(
                perAppKey(policy.packageName, PreferenceContract.KEY_USE_GLOBAL),
                policy.useGlobal,
            )
        writeConfig(
            editor = editor,
            keyFor = { key -> perAppKey(policy.packageName, key) },
            config = policy.config,
        ).commit()
    }

    private fun readConfig(
        keyFor: (String) -> String,
        fallback: ModuleConfig,
    ): ModuleConfig {
        val delayKey = keyFor(PreferenceContract.KEY_DELAY)
        val fpsKey = keyFor(PreferenceContract.KEY_FPS)
        val scaleKey = keyFor(PreferenceContract.KEY_SCALE)
        val displayModeKey = keyFor(PreferenceContract.KEY_DISPLAY_MODE_ID)
        val delay = readInt(delayKey, fallback.delay)
        val fps = readInt(fpsKey, fallback.fps)
        val scale = readFloat(scaleKey, fallback.scale)
        val displayModeId = readInt(displayModeKey, fallback.displayModeId)

        return ModuleConfig(
            delay = delay.takeIf { it > 0 } ?: ModuleConfig.DEFAULT_DELAY,
            delayEnabled = readEnabled(
                keyFor(PreferenceContract.KEY_DELAY_ENABLED),
                delayKey,
                fallback.delayEnabled,
                delay > 0,
            ),
            fps = fps.takeIf { it > 0 } ?: ModuleConfig.DEFAULT_FPS,
            fpsEnabled = readEnabled(
                keyFor(PreferenceContract.KEY_FPS_ENABLED),
                fpsKey,
                fallback.fpsEnabled,
                fps > 0,
            ),
            scale = scale.takeIf { it > 0f } ?: ModuleConfig.DEFAULT_SCALE,
            scaleEnabled = readEnabled(
                keyFor(PreferenceContract.KEY_SCALE_ENABLED),
                scaleKey,
                fallback.scaleEnabled,
                scale > 0f,
            ),
            displayModeId = displayModeId,
            displayModeEnabled = readEnabled(
                keyFor(PreferenceContract.KEY_DISPLAY_MODE_ENABLED),
                displayModeKey,
                fallback.displayModeEnabled,
                displayModeId > 0,
            ),
            modOpcode = preferences.getBoolean(
                keyFor(PreferenceContract.KEY_MOD_OPCODE),
                fallback.modOpcode,
            ),
        )
    }

    private fun readEnabled(
        enabledKey: String,
        legacyValueKey: String,
        fallback: Boolean,
        legacyValue: Boolean,
    ): Boolean = when {
        preferences.contains(enabledKey) -> preferences.getBoolean(enabledKey, fallback)
        preferences.contains(legacyValueKey) -> legacyValue
        else -> fallback
    }

    private fun writeConfig(
        editor: SharedPreferences.Editor,
        keyFor: (String) -> String,
        config: ModuleConfig,
    ): SharedPreferences.Editor = editor
        .putString(keyFor(PreferenceContract.KEY_DELAY), config.delay.toString())
        .putBoolean(keyFor(PreferenceContract.KEY_DELAY_ENABLED), config.delayEnabled)
        .putString(keyFor(PreferenceContract.KEY_FPS), config.fps.toString())
        .putBoolean(keyFor(PreferenceContract.KEY_FPS_ENABLED), config.fpsEnabled)
        .putString(keyFor(PreferenceContract.KEY_SCALE), config.scale.toString())
        .putBoolean(keyFor(PreferenceContract.KEY_SCALE_ENABLED), config.scaleEnabled)
        .putString(
            keyFor(PreferenceContract.KEY_DISPLAY_MODE_ID),
            config.displayModeId.toString(),
        )
        .putBoolean(
            keyFor(PreferenceContract.KEY_DISPLAY_MODE_ENABLED),
            config.displayModeEnabled,
        )
        .putBoolean(keyFor(PreferenceContract.KEY_MOD_OPCODE), config.modOpcode)

    private fun readInt(key: String, fallback: Int): Int =
        preferences.getString(key, fallback.toString())?.toIntOrNull() ?: fallback

    private fun readFloat(key: String, fallback: Float): Float =
        preferences.getString(key, fallback.toString())?.toFloatOrNull() ?: fallback

    private fun ensureHookLogToken() {
        if (!preferences.getString(KEY_HOOK_LOG_TOKEN, null).isNullOrBlank()) return
        preferences.edit()
            .putString(KEY_HOOK_LOG_TOKEN, UUID.randomUUID().toString())
            .commit()
    }

    private fun migrateLegacyPerAppEntries() {
        val discovered = preferences.all.keys.mapNotNull { key ->
            CONFIG_KEYS.firstNotNullOfOrNull { configKey ->
                val suffix = "_per_app_$configKey"
                key.takeIf { it.endsWith(suffix) }
                    ?.removeSuffix(suffix)
                    ?.takeIf(PreferenceContract::isValidPackageName)
            }
        }.toSet()
        if (discovered.isEmpty()) return

        val current = managedPackages()
        val missing = discovered - current
        if (missing.isEmpty()) return

        val editor = preferences.edit()
            .putStringSet(KEY_MANAGED_PACKAGES, current + missing)
        missing.forEach { packageName ->
            editor.putBoolean(perAppKey(packageName, PreferenceContract.KEY_ENABLED), true)
            editor.putBoolean(perAppKey(packageName, PreferenceContract.KEY_USE_GLOBAL), false)
        }
        editor.commit()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    private fun openPreferences(context: Context): SharedPreferences = try {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
    } catch (throwable: Throwable) {
        Log.w(TAG, "World-readable preferences are provided by the Xposed framework", throwable)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
