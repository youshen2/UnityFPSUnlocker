package io.github.hexstr.UnityFPSUnlocker.xposed;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Constructor;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hexstr.UnityFPSUnlocker.logging.HookEventDispatcher;
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract;

public final class DisplayModeHook {
    private DisplayModeHook() {}

    public static void install(
            Class<?> unityPlayerClass,
            XC_LoadPackage.LoadPackageParam loadPackage,
            RuntimeConfig config) {
        try {
            Class<?> lifecycleEventsClass = XposedHelpers.findClass(
                    "com.unity3d.player.IUnityPlayerLifecycleEvents",
                    loadPackage.classLoader);
            boolean hooked = false;

            for (Constructor<?> constructor : unityPlayerClass.getDeclaredConstructors()) {
                if (!containsParameter(constructor, lifecycleEventsClass)) continue;
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = findActivity(param.args);
                        if (activity == null) {
                            HookEventDispatcher.record(
                                    loadPackage.packageName,
                                    HookLogContract.STATUS_FAILED,
                                    HookLogContract.MESSAGE_DISPLAY_MODE_ACTIVITY_NOT_FOUND);
                            return;
                        }

                        HookEventDispatcher.setContext(activity);
                        Window window = activity.getWindow();
                        WindowManager.LayoutParams attributes = window.getAttributes();
                        attributes.preferredDisplayModeId = config.displayModeId;
                        window.setAttributes(attributes);
                        HookEventDispatcher.record(
                                loadPackage.packageName,
                                HookLogContract.STATUS_SUCCESS,
                                HookLogContract.FIELD_DISPLAY_MODE_ID + config.displayModeId);
                    }
                });
                hooked = true;
            }

            if (!hooked) {
                HookEventDispatcher.record(
                        loadPackage.packageName,
                        HookLogContract.STATUS_FAILED,
                        HookLogContract.MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND);
            }
        } catch (Throwable error) {
            HookEventDispatcher.record(
                    loadPackage.packageName,
                    HookLogContract.STATUS_FAILED,
                    HookLogContract.MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION);
            XposedBridge.log(error);
        }
    }

    private static boolean containsParameter(
            Constructor<?> constructor,
            Class<?> targetType) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (parameterType.equals(targetType)) return true;
        }
        return false;
    }

    private static Activity findActivity(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof Activity) return (Activity) argument;
        }
        return null;
    }
}
