package io.github.hexstr.UnityFPSUnlocker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hexstr.UnityFPSUnlocker.logging.HookEventDispatcher;
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract;
import io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract;
import io.github.hexstr.UnityFPSUnlocker.xposed.DisplayModeHook;
import io.github.hexstr.UnityFPSUnlocker.xposed.RuntimeConfig;
import io.github.hexstr.UnityFPSUnlocker.xposed.RuntimeConfigResolver;

public class MyModule implements IXposedHookLoadPackage {
    public static native void startNativeHook(
            String packageName,
            int delay,
            int fps,
            boolean modifyOpcode,
            float scale);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackage) {
        String packageName = loadPackage.packageName;
        if (BuildConfig.APPLICATION_ID.equals(packageName)) return;

        XSharedPreferences settings = getPreferences();
        HookEventDispatcher.setToken(settings);
        boolean managed = RuntimeConfigResolver.isManaged(settings, packageName);
        RuntimeConfig config = RuntimeConfigResolver.resolve(settings, packageName, managed);
        if (config == null) {
            if (managed) {
                recordEvent(
                        packageName,
                        HookLogContract.STATUS_SKIPPED,
                        HookLogContract.MESSAGE_APP_POLICY);
            }
            return;
        }

        boolean modifyDisplayMode = config.displayModeId > 0;
        boolean initializeNative = config.fps > 0 || config.scale > 0;
        if (!modifyDisplayMode && !initializeNative) {
            if (managed) {
                recordEvent(
                        packageName,
                        HookLogContract.STATUS_SKIPPED,
                        HookLogContract.MESSAGE_NO_ENABLED_PARAMETERS);
            }
            return;
        }

        Class<?> unityPlayerClass;
        try {
            unityPlayerClass = XposedHelpers.findClass(
                    "com.unity3d.player.UnityPlayer",
                    loadPackage.classLoader);
        } catch (Throwable error) {
            if (managed) {
                recordEvent(
                        packageName,
                        HookLogContract.STATUS_FAILED,
                        HookLogContract.MESSAGE_UNITY_PLAYER_CLASS_NOT_FOUND);
            }
            XposedBridge.log(error);
            return;
        }

        HookEventDispatcher.installContextHook();
        if (modifyDisplayMode) {
            DisplayModeHook.install(unityPlayerClass, loadPackage, config);
        }
        if (initializeNative) {
            startNative(packageName, config);
        }
    }

    public static void recordNativeHookEvent(
            String packageName,
            String status,
            String message) {
        HookEventDispatcher.record(packageName, status, message);
    }

    private static XSharedPreferences getPreferences() {
        XSharedPreferences preferences = new XSharedPreferences(
                BuildConfig.APPLICATION_ID,
                PreferenceContract.PREFS_NAME);
        preferences.reload();
        return preferences.getFile().canRead() ? preferences : null;
    }

    private static void recordEvent(String packageName, String status, String message) {
        HookEventDispatcher.installContextHook();
        HookEventDispatcher.record(packageName, status, message);
    }

    private static void startNative(String packageName, RuntimeConfig config) {
        XposedBridge.log(
                "UnityFPSUnlocker: package=" + packageName
                        + " | display_mode_id=" + config.displayModeId
                        + " | delay=" + config.delay
                        + " | fps=" + config.fps
                        + " | modify_opcode=" + config.modifyOpcode
                        + " | scale=" + config.scale);
        try {
            System.loadLibrary("UnityFPSUnlocker");
            startNativeHook(
                    packageName,
                    config.delay,
                    config.fps,
                    config.modifyOpcode,
                    config.scale);
        } catch (Throwable error) {
            HookEventDispatcher.record(
                    packageName,
                    HookLogContract.STATUS_FAILED,
                    HookLogContract.MESSAGE_NATIVE_INITIALIZATION);
            XposedBridge.log(error);
        }
    }
}
