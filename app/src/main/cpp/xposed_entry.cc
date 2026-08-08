#include <jni.h>

#include <thread>

#include "fpslimiter.hh"
#include "logging/hook_event_reporter.hh"
#include "utility/config.hh"
#include "utility/logger.hh"

extern "C" JNIEXPORT void JNICALL
Java_io_github_hexstr_UnityFPSUnlocker_MyModule_startNativeHook(
    JNIEnv* env,
    jclass clazz,
    jstring package_name,
    jint delay,
    jint fps,
    jboolean modify_opcode,
    jfloat scale) {
    if (!HookEventReporter::Initialize(env, clazz)) {
        ERROR("Failed to initialize native hook event reporter");
    }
    const char* package_chars = env->GetStringUTFChars(package_name, nullptr);
    const std::string package_value = package_chars ? package_chars : "unknown";
    if (package_chars) {
        env->ReleaseStringUTFChars(package_name, package_chars);
    }

    LOG(
        "[UnityFPSUnlocker][xposed] package: %s | delay: %d | fps: %d | mod_opcode: %d | scale: %f",
        package_value.c_str(),
        delay,
        fps,
        modify_opcode,
        scale);

    const ConfigValue config(
        package_value,
        delay,
        fps,
        modify_opcode == JNI_TRUE,
        scale);
    std::thread([config] {
        FPSLimiter::Start(config);
    }).detach();
}

JNIEXPORT jint JNI_OnLoad(JavaVM*, void*) {
    return JNI_VERSION_1_6;
}
