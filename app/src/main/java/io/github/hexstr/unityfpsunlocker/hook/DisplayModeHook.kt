package io.github.hexstr.UnityFPSUnlocker.hook

import android.app.Activity
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract

/**
 * 在 UnityPlayer 构造完成后，把承载游戏画面的 Activity 窗口切换到指定显示模式。
 *
 * Unity 会把 Activity 作为构造参数传入；不同 Unity 版本还额外要求
 * IUnityPlayerLifecycleEvents，因此按“参数中包含生命周期接口”筛选构造函数。
 */
class DisplayModeHook(
    private val module: XposedModule,
    private val classLoader: ClassLoader,
    private val unityPlayerClass: Class<*>,
    private val packageName: String,
    private val displayModeId: Int,
) {

    fun install() {
        try {
            val lifecycleClass = runCatching {
                Class.forName(LIFECYCLE_EVENTS, false, classLoader)
            }.getOrNull()

            if (lifecycleClass == null) {
                reportFailure(HookLogContract.MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND)
                return
            }

            val constructors = unityPlayerClass.declaredConstructors.filter { constructor ->
                constructor.parameterTypes.any { it == lifecycleClass }
            }

            if (constructors.isEmpty()) {
                reportFailure(HookLogContract.MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND)
                return
            }

            constructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    val activity = chain.args.firstOrNull { it is Activity } as? Activity
                    if (activity == null) {
                        reportFailure(HookLogContract.MESSAGE_DISPLAY_MODE_ACTIVITY_NOT_FOUND)
                    } else {
                        applyDisplayMode(activity)
                    }
                    result
                }
            }
        } catch (throwable: Throwable) {
            module.log(Log.ERROR, MainHook.TAG, "Failed to install display mode hook", throwable)
            reportFailure(HookLogContract.MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION)
        }
    }

    private fun applyDisplayMode(activity: Activity) {
        runCatching {
            val attributes = activity.window.attributes
            attributes.preferredDisplayModeId = displayModeId
            activity.window.attributes = attributes
        }.onSuccess {
            HookEventDispatcher.setContext(activity)
            HookEventDispatcher.report(
                packageName,
                HookLogContract.STATUS_SUCCESS,
                HookLogContract.FIELD_DISPLAY_MODE_ID + displayModeId,
            )
        }.onFailure {
            module.log(Log.ERROR, MainHook.TAG, "Failed to set display mode", it)
            reportFailure(HookLogContract.MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION)
        }
    }

    private fun reportFailure(message: String) {
        HookEventDispatcher.report(packageName, HookLogContract.STATUS_FAILED, message)
    }

    private companion object {
        const val LIFECYCLE_EVENTS = "com.unity3d.player.IUnityPlayerLifecycleEvents"
    }
}
