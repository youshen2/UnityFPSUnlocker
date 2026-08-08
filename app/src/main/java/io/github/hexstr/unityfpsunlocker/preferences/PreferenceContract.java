package io.github.hexstr.UnityFPSUnlocker.preferences;

import java.util.regex.Pattern;

public final class PreferenceContract {
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");

    public static final String PREFS_NAME = "fps_prefs";
    public static final String KEY_MANAGED_PACKAGES = "managed_packages";
    public static final String KEY_ONLY_MANAGED_APPS = "only_managed_apps";
    public static final String KEY_HOOK_LOG_TOKEN = "hook_log_token";
    public static final String KEY_DELAY = "delay";
    public static final String KEY_DELAY_ENABLED = "delay_enabled";
    public static final String KEY_FPS = "fps";
    public static final String KEY_FPS_ENABLED = "fps_enabled";
    public static final String KEY_SCALE = "scale";
    public static final String KEY_SCALE_ENABLED = "scale_enabled";
    public static final String KEY_DISPLAY_MODE_ID = "display_mode_id";
    public static final String KEY_DISPLAY_MODE_ENABLED = "display_mode_enabled";
    public static final String KEY_MOD_OPCODE = "mod_opcode";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_USE_GLOBAL = "use_global";

    private PreferenceContract() {}

    public static String perAppKey(String packageName, String key) {
        return packageName + "_per_app_" + key;
    }

    public static boolean isValidPackageName(String packageName) {
        return packageName != null && PACKAGE_NAME_PATTERN.matcher(packageName).matches();
    }
}
