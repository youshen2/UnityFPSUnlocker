package io.github.hexstr.UnityFPSUnlocker.logging;

public final class HookLogContract {
    public static final String ACTION =
            "io.github.hexstr.UnityFPSUnlocker.action.RECORD_HOOK_EVENT";
    public static final String RECEIVER_CLASS =
            "io.github.hexstr.UnityFPSUnlocker.logging.HookLogReceiver";
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_PROTOCOL_TOKEN = "protocol_token";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String MESSAGE_APP_POLICY = "reason=app_policy";
    public static final String MESSAGE_NO_ENABLED_PARAMETERS =
            "reason=no_enabled_parameters";
    public static final String MESSAGE_UNITY_PLAYER_CLASS_NOT_FOUND =
            "reason=unity_player_class_not_found";
    public static final String MESSAGE_NATIVE_INITIALIZATION =
            "reason=native_initialization";
    public static final String MESSAGE_UNITY_ICALL_UNAVAILABLE =
            "reason=unity_icall_unavailable";
    public static final String MESSAGE_LIBIL2CPP_NOT_FOUND =
            "reason=libil2cpp_not_found";
    public static final String MESSAGE_DISPLAY_MODE_ACTIVITY_NOT_FOUND =
            "reason=display_mode_activity_not_found";
    public static final String MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND =
            "reason=display_mode_constructor_not_found";
    public static final String MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION =
            "reason=display_mode_hook_initialization";
    public static final String FIELD_DISPLAY_MODE_ID = "display_mode_id=";
    public static final String FIELD_FPS = "fps=";
    public static final String FIELD_RESOLUTION_SCALE = "resolution_scale=";

    private HookLogContract() {}
}
