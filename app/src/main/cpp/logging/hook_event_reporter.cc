#include "hook_event_reporter.hh"

#include "utility/logger.hh"

namespace {
JavaVM* java_vm = nullptr;
jclass module_class = nullptr;
jmethodID record_event_method = nullptr;
}

bool HookEventReporter::Initialize(JNIEnv* env, jclass clazz) {
    if (java_vm != nullptr && module_class != nullptr && record_event_method != nullptr) {
        return true;
    }
    if (env->GetJavaVM(&java_vm) != JNI_OK) {
        return false;
    }

    module_class = static_cast<jclass>(env->NewGlobalRef(clazz));
    if (module_class == nullptr) {
        return false;
    }
    record_event_method = env->GetStaticMethodID(
        clazz,
        "recordNativeHookEvent",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (record_event_method != nullptr) {
        return true;
    }
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    env->DeleteGlobalRef(module_class);
    module_class = nullptr;
    return false;
}

void HookEventReporter::Report(
    const char* package_name,
    const char* status,
    const char* message) {
    if (java_vm == nullptr || module_class == nullptr || record_event_method == nullptr) {
        return;
    }

    JNIEnv* env = nullptr;
    bool attached_here = false;
    if (java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (java_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        attached_here = true;
    }

    jstring package_value = env->NewStringUTF(package_name);
    jstring status_value = env->NewStringUTF(status);
    jstring message_value = env->NewStringUTF(message);
    if (package_value != nullptr && status_value != nullptr && message_value != nullptr) {
        env->CallStaticVoidMethod(
            module_class,
            record_event_method,
            package_value,
            status_value,
            message_value);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            ERROR("Failed to deliver native hook event to Java");
        }
    }

    if (package_value != nullptr) env->DeleteLocalRef(package_value);
    if (status_value != nullptr) env->DeleteLocalRef(status_value);
    if (message_value != nullptr) env->DeleteLocalRef(message_value);
    if (attached_here) java_vm->DetachCurrentThread();
}
