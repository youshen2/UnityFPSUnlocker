package io.github.hexstr.UnityFPSUnlocker.logging

/** 被 Hook 进程向模块进程回传事件的协议常量。 */
object HookLogContract {
    const val ACTION = "io.github.hexstr.UnityFPSUnlocker.action.RECORD_HOOK_EVENT"
    const val RECEIVER_CLASS = "io.github.hexstr.UnityFPSUnlocker.logging.HookLogReceiver"

    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_STATUS = "status"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_TOKEN = "protocol_token"

    const val STATUS_SUCCESS = "success"
    const val STATUS_FAILED = "failed"
    const val STATUS_SKIPPED = "skipped"

    const val MESSAGE_APP_POLICY = "reason=app_policy"
    const val MESSAGE_NO_ENABLED_PARAMETERS = "reason=no_enabled_parameters"
    const val MESSAGE_UNITY_PLAYER_NOT_FOUND = "reason=unity_player_not_found"
    const val MESSAGE_UNITY_PLAYER_CLASS_NOT_FOUND = "reason=unity_player_class_not_found"
    const val MESSAGE_NATIVE_INITIALIZATION = "reason=native_initialization"
    const val MESSAGE_UNITY_ICALL_UNAVAILABLE = "reason=unity_icall_unavailable"
    const val MESSAGE_LIBIL2CPP_NOT_FOUND = "reason=libil2cpp_not_found"
    const val MESSAGE_DISPLAY_MODE_ACTIVITY_NOT_FOUND = "reason=display_mode_activity_not_found"
    const val MESSAGE_DISPLAY_MODE_CONSTRUCTOR_NOT_FOUND = "reason=display_mode_constructor_not_found"
    const val MESSAGE_DISPLAY_MODE_FAILED = "reason=display_mode_failed"
    const val MESSAGE_DISPLAY_MODE_HOOK_INITIALIZATION = "reason=display_mode_hook_initialization"

    const val FIELD_DISPLAY_MODE_ID = "display_mode_id="
    const val FIELD_FPS = "fps="
    const val FIELD_RESOLUTION_SCALE = "resolution_scale="
}
