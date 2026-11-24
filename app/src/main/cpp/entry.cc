#include <jni.h>

#include <thread>

#include "config.hh"
#include "event_reporter.hh"
#include "fps_hook.hh"
#include "log.hh"

extern "C" JNIEXPORT void JNICALL
Java_io_github_hexstr_UnityFPSUnlocker_nativebridge_NativeBridge_startNative(
    JNIEnv* env,
    jclass clazz,
    jstring package_name,
    jint delay,
    jint fps,
    jboolean modify_opcode,
    jfloat scale) {
    if (!ufu::InitializeEventReporter(env, clazz)) {
        UFU_ERR("Failed to initialize event reporter");
    }

    const char* package_chars = env->GetStringUTFChars(package_name, nullptr);
    std::string package = package_chars != nullptr ? package_chars : "unknown";
    if (package_chars != nullptr) {
        env->ReleaseStringUTFChars(package_name, package_chars);
    }

    UFU_LOG(
        "startNative: package=%s delay=%d fps=%d modify_opcode=%d scale=%.3f",
        package.c_str(),
        delay,
        fps,
        modify_opcode == JNI_TRUE ? 1 : 0,
        scale);

    const ufu::Config config(
        std::move(package),
        delay,
        fps,
        modify_opcode == JNI_TRUE,
        scale);

    std::thread([config] { ufu::Run(config); }).detach();
}

JNIEXPORT jint JNI_OnLoad(JavaVM*, void*) {
    return JNI_VERSION_1_6;
}
