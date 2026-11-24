package io.github.hexstr.UnityFPSUnlocker.config

/** 模块与被 Hook 进程共享的远程配置组。 */
const val PREFS_GROUP = "unity_fps_unlocker"

const val KEY_FPS = "fps"
const val KEY_FPS_ENABLED = "fps_enabled"
const val KEY_DELAY = "delay"
const val KEY_DELAY_ENABLED = "delay_enabled"
const val KEY_SCALE = "scale"
const val KEY_SCALE_ENABLED = "scale_enabled"
const val KEY_DISPLAY_MODE_ID = "display_mode_id"
const val KEY_DISPLAY_MODE_ENABLED = "display_mode_enabled"
const val KEY_MODIFY_OPCODE = "patch_opcode"

const val KEY_ONLY_MANAGED = "only_managed_apps_v2"
const val KEY_MANAGED_APPS = "managed_apps"
const val KEY_HOOK_LOG_TOKEN = "hook_log_token"

const val KEY_APP_ENABLED = "enabled"
const val KEY_APP_USE_GLOBAL = "use_global_v2"

fun appKey(packageName: String, key: String): String = "app.$packageName.$key"
