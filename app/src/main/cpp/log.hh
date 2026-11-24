#pragma once

#include <android/log.h>

#define UFU_TAG "UnityFPSUnlocker"
#define UFU_LOG(...) __android_log_print(ANDROID_LOG_DEBUG, UFU_TAG, __VA_ARGS__)
#define UFU_ERR(...) __android_log_print(ANDROID_LOG_ERROR, UFU_TAG, __VA_ARGS__)
