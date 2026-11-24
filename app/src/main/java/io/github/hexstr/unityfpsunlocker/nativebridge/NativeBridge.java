package io.github.hexstr.UnityFPSUnlocker.nativebridge;

import io.github.hexstr.UnityFPSUnlocker.hook.HookEventDispatcher;

/**
 * Java 侧的 JNI 桥。
 *
 * 类名和方法名与 cpp/entry.cc 中的符号及 GetStaticMethodID 保持一致；R8 规则会保留整个类。
 */
public final class NativeBridge {

    private NativeBridge() {}

    public static native void startNative(
            String packageName,
            int delay,
            int fps,
            boolean modifyOpcode,
            float scale);

    /** 由 native 线程回调，把 Hook 结果回传到 Java 侧日志分发器。 */
    public static void reportNativeEvent(
            String packageName,
            String status,
            String message) {
        HookEventDispatcher.report(packageName, status, message);
    }
}
