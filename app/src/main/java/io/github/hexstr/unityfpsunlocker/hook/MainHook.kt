package io.github.hexstr.UnityFPSUnlocker.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.hexstr.UnityFPSUnlocker.BuildConfig
import io.github.hexstr.UnityFPSUnlocker.config.ConfigStore
import io.github.hexstr.UnityFPSUnlocker.config.PREFS_GROUP
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract

/**
 * libxposed API 101 模块入口。
 *
 * 由 META-INF/xposed/java_init.list 注册；作用域由用户在模块界面中通过
 * XposedService.requestScope 动态申请。作用域内的每个进程都会触发回调，
 * 因此这里按包名校验配置，而不是假设回调一定来自目标应用。
 */
class MainHook : XposedModule() {

    companion object {
        const val TAG = "UnityFPSUnlocker"
        const val UNITY_PLAYER = "com.unity3d.player.UnityPlayer"

        lateinit var instance: MainHook
            private set
    }

    init {
        instance = this
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "Loaded into ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.packageName
        if (packageName == BuildConfig.APPLICATION_ID) return

        // 只处理主包，避免同一进程内 createPackageContext 等场景重复注入
        if (!param.isFirstPackage) return

        val prefs = runCatching { getRemotePreferences(PREFS_GROUP) }.getOrNull()
        if (prefs == null) {
            log(Log.WARN, TAG, "Remote preferences unavailable in $packageName")
            return
        }

        val store = ConfigStore(prefs)
        HookEventDispatcher.initialize(prefs)
        HookEventDispatcher.install(this, param.classLoader)

        val managed = store.isManaged(packageName)
        val config = store.resolveForPackage(packageName)
        if (config == null) {
            if (managed) {
                HookEventDispatcher.report(
                    packageName,
                    HookLogContract.STATUS_SKIPPED,
                    HookLogContract.MESSAGE_APP_POLICY,
                )
            }
            return
        }

        if (!config.hasAnyParam) {
            if (managed) {
                HookEventDispatcher.report(
                    packageName,
                    HookLogContract.STATUS_SKIPPED,
                    HookLogContract.MESSAGE_NO_ENABLED_PARAMETERS,
                )
            }
            return
        }

        val unityPlayer = runCatching {
            Class.forName(UNITY_PLAYER, false, param.classLoader)
        }.getOrNull()

        if (unityPlayer == null) {
            if (managed) {
                HookEventDispatcher.report(
                    packageName,
                    HookLogContract.STATUS_FAILED,
                    HookLogContract.MESSAGE_UNITY_PLAYER_CLASS_NOT_FOUND,
                )
            }
            return
        }

        log(
            Log.INFO,
            TAG,
            "Applying to $packageName: fps=${config.effectiveFps} " +
                "delay=${config.effectiveDelay} scale=${config.effectiveScale} " +
                "displayMode=${config.effectiveDisplayModeId}",
        )

        if (config.effectiveDisplayModeId > 0) {
            DisplayModeHook(
                module = this,
                classLoader = param.classLoader,
                unityPlayerClass = unityPlayer,
                packageName = packageName,
                displayModeId = config.effectiveDisplayModeId,
            ).install()
        }

        if (config.hasNativeParams) {
            NativeHook.start(
                module = this,
                packageName = packageName,
                delay = config.effectiveDelay,
                fps = config.effectiveFps,
                modifyOpcode = config.modifyOpcode,
                scale = config.effectiveScale,
            )
        }
    }
}
