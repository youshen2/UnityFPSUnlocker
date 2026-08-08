package io.github.hexstr.UnityFPSUnlocker.xposed;

import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_DELAY;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_DELAY_ENABLED;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_DISPLAY_MODE_ID;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_DISPLAY_MODE_ENABLED;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_ENABLED;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_FPS;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_FPS_ENABLED;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_MANAGED_PACKAGES;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_MOD_OPCODE;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_ONLY_MANAGED_APPS;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_SCALE;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_SCALE_ENABLED;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.KEY_USE_GLOBAL;
import static io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract.perAppKey;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import io.github.hexstr.UnityFPSUnlocker.ModuleConfig;

public final class RuntimeConfigResolver {
    private RuntimeConfigResolver() {}

    public static boolean isManaged(XSharedPreferences settings, String packageName) {
        if (settings == null) return false;
        Set<String> managedPackages = settings.getStringSet(
                KEY_MANAGED_PACKAGES,
                Collections.<String>emptySet());
        return managedPackages != null && managedPackages.contains(packageName);
    }

    public static RuntimeConfig resolve(
            XSharedPreferences settings,
            String packageName,
            boolean managed) {
        int displayModeId = ModuleConfig.DEFAULT_DISPLAY_MODE_ID;
        int delay = ModuleConfig.DEFAULT_DELAY;
        int fps = ModuleConfig.DEFAULT_FPS;
        boolean modifyOpcode = ModuleConfig.DEFAULT_MOD_OPCODE;
        float scale = ModuleConfig.DEFAULT_SCALE;
        boolean displayModeEnabled = ModuleConfig.DEFAULT_DISPLAY_MODE_ENABLED;
        boolean delayEnabled = ModuleConfig.DEFAULT_DELAY_ENABLED;
        boolean fpsEnabled = ModuleConfig.DEFAULT_FPS_ENABLED;
        boolean scaleEnabled = ModuleConfig.DEFAULT_SCALE_ENABLED;

        if (settings == null) {
            XposedBridge.log("UnityFPSUnlocker: cannot read settings; using defaults");
            return effectiveConfig(
                    displayModeId,
                    displayModeEnabled,
                    delay,
                    delayEnabled,
                    fps,
                    fpsEnabled,
                    modifyOpcode,
                    scale,
                    scaleEnabled);
        }
        if (!managed && settings.getBoolean(KEY_ONLY_MANAGED_APPS, false)) {
            return null;
        }
        if (managed && !settings.getBoolean(perAppKey(packageName, KEY_ENABLED), true)) {
            return null;
        }

        displayModeId = getInt(settings, KEY_DISPLAY_MODE_ID, displayModeId);
        delay = getInt(settings, KEY_DELAY, delay);
        fps = getInt(settings, KEY_FPS, fps);
        modifyOpcode = settings.getBoolean(KEY_MOD_OPCODE, modifyOpcode);
        scale = getFloat(settings, KEY_SCALE, scale);
        displayModeEnabled = getEnabled(
                settings,
                KEY_DISPLAY_MODE_ENABLED,
                KEY_DISPLAY_MODE_ID,
                displayModeEnabled,
                displayModeId > 0);
        delayEnabled = getEnabled(
                settings,
                KEY_DELAY_ENABLED,
                KEY_DELAY,
                delayEnabled,
                delay > 0);
        fpsEnabled = getEnabled(
                settings,
                KEY_FPS_ENABLED,
                KEY_FPS,
                fpsEnabled,
                fps > 0);
        scaleEnabled = getEnabled(
                settings,
                KEY_SCALE_ENABLED,
                KEY_SCALE,
                scaleEnabled,
                scale > 0);

        boolean useGlobal = managed
                && settings.getBoolean(perAppKey(packageName, KEY_USE_GLOBAL), false);
        if (managed && !useGlobal) {
            displayModeId = getInt(
                    settings,
                    perAppKey(packageName, KEY_DISPLAY_MODE_ID),
                    displayModeId);
            delay = getInt(settings, perAppKey(packageName, KEY_DELAY), delay);
            fps = getInt(settings, perAppKey(packageName, KEY_FPS), fps);
            modifyOpcode = settings.getBoolean(
                    perAppKey(packageName, KEY_MOD_OPCODE),
                    modifyOpcode);
            scale = getFloat(settings, perAppKey(packageName, KEY_SCALE), scale);
            displayModeEnabled = getEnabled(
                    settings,
                    perAppKey(packageName, KEY_DISPLAY_MODE_ENABLED),
                    perAppKey(packageName, KEY_DISPLAY_MODE_ID),
                    displayModeEnabled,
                    displayModeId > 0);
            delayEnabled = getEnabled(
                    settings,
                    perAppKey(packageName, KEY_DELAY_ENABLED),
                    perAppKey(packageName, KEY_DELAY),
                    delayEnabled,
                    delay > 0);
            fpsEnabled = getEnabled(
                    settings,
                    perAppKey(packageName, KEY_FPS_ENABLED),
                    perAppKey(packageName, KEY_FPS),
                    fpsEnabled,
                    fps > 0);
            scaleEnabled = getEnabled(
                    settings,
                    perAppKey(packageName, KEY_SCALE_ENABLED),
                    perAppKey(packageName, KEY_SCALE),
                    scaleEnabled,
                    scale > 0);
        }

        return effectiveConfig(
                displayModeId,
                displayModeEnabled,
                delay,
                delayEnabled,
                fps,
                fpsEnabled,
                modifyOpcode,
                scale,
                scaleEnabled);
    }

    private static RuntimeConfig effectiveConfig(
            int displayModeId,
            boolean displayModeEnabled,
            int delay,
            boolean delayEnabled,
            int fps,
            boolean fpsEnabled,
            boolean modifyOpcode,
            float scale,
            boolean scaleEnabled) {
        return new RuntimeConfig(
                displayModeEnabled ? displayModeId : ModuleConfig.NO_CHANGE_DISPLAY_MODE_ID,
                delayEnabled ? delay : 0,
                fpsEnabled ? fps : ModuleConfig.NO_CHANGE_FPS,
                modifyOpcode,
                scaleEnabled ? scale : ModuleConfig.NO_CHANGE_SCALE);
    }

    private static boolean getEnabled(
            XSharedPreferences settings,
            String enabledKey,
            String legacyValueKey,
            boolean fallback,
            boolean legacyValue) {
        if (settings.contains(enabledKey)) {
            return settings.getBoolean(enabledKey, fallback);
        }
        return settings.contains(legacyValueKey) ? legacyValue : fallback;
    }

    private static int getInt(XSharedPreferences settings, String key, int fallback) {
        try {
            return Integer.parseInt(settings.getString(key, String.valueOf(fallback)));
        } catch (NumberFormatException error) {
            XposedBridge.log("Invalid integer preference value for " + key);
            return fallback;
        }
    }

    private static float getFloat(XSharedPreferences settings, String key, float fallback) {
        try {
            return Float.parseFloat(settings.getString(key, String.valueOf(fallback)));
        } catch (NumberFormatException error) {
            XposedBridge.log("Invalid float preference value for " + key);
            return fallback;
        }
    }
}
