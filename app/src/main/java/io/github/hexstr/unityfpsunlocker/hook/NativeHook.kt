package io.github.hexstr.UnityFPSUnlocker.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract
import io.github.hexstr.UnityFPSUnlocker.nativebridge.NativeBridge
import java.io.File

/** 负责把模块 APK 中的 native 库加载进目标进程并启动 IL2CPP Hook。 */
object NativeHook {

    private const val TAG = MainHook.TAG
    private const val LIBRARY = "UnityFPSUnlocker"

    @Volatile
    private var loaded = false

    fun start(
        module: XposedModule,
        packageName: String,
        delay: Int,
        fps: Int,
        modifyOpcode: Boolean,
        scale: Float,
    ) {
        if (!ensureLoaded(module)) {
            HookEventDispatcher.report(
                packageName,
                HookLogContract.STATUS_FAILED,
                HookLogContract.MESSAGE_NATIVE_INITIALIZATION,
            )
            return
        }

        runCatching {
            NativeBridge.startNative(packageName, delay, fps, modifyOpcode, scale)
        }.onFailure {
            module.log(Log.ERROR, TAG, "Failed to start native hook", it)
            HookEventDispatcher.report(
                packageName,
                HookLogContract.STATUS_FAILED,
                HookLogContract.MESSAGE_NATIVE_INITIALIZATION,
            )
        }
    }

    private fun ensureLoaded(module: XposedModule): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true

            if (runCatching { System.loadLibrary(LIBRARY) }.isSuccess) {
                loaded = true
                return true
            }

            val directLoad = runCatching {
                val directory = module.moduleApplicationInfo.nativeLibraryDir
                System.load(File(directory, "lib$LIBRARY.so").absolutePath)
            }.isSuccess

            if (directLoad) {
                loaded = true
            } else {
                module.log(Log.ERROR, TAG, "Unable to load lib$LIBRARY.so")
            }
            return loaded
        }
    }
}
