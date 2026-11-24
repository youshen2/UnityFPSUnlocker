#include "event_reporter.hh"

#include "log.hh"

namespace ufu {
namespace {

JavaVM* g_vm = nullptr;
jclass g_bridge_class = nullptr;
jmethodID g_report_method = nullptr;

} // namespace

bool InitializeEventReporter(JNIEnv* env, jclass bridge_class) {
    if (g_vm != nullptr && g_bridge_class != nullptr && g_report_method != nullptr) {
        return true;
    }

    if (env->GetJavaVM(&g_vm) != JNI_OK) {
        return false;
    }

    g_bridge_class = static_cast<jclass>(env->NewGlobalRef(bridge_class));
    if (g_bridge_class == nullptr) {
        return false;
    }

    g_report_method = env->GetStaticMethodID(
        bridge_class,
        "reportNativeEvent",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (g_report_method != nullptr) {
        return true;
    }

    if (env->ExceptionCheck()) env->ExceptionClear();
    env->DeleteGlobalRef(g_bridge_class);
    g_bridge_class = nullptr;
    return false;
}

void ReportEvent(const char* package_name, const char* status, const char* message) {
    if (g_vm == nullptr || g_bridge_class == nullptr || g_report_method == nullptr) {
        return;
    }

    JNIEnv* env = nullptr;
    bool attached_here = false;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached_here = true;
    }

    jstring package = env->NewStringUTF(package_name);
    jstring status_value = env->NewStringUTF(status);
    jstring message_value = env->NewStringUTF(message);

    if (package != nullptr && status_value != nullptr && message_value != nullptr) {
        env->CallStaticVoidMethod(g_bridge_class, g_report_method, package, status_value, message_value);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            UFU_ERR("Failed to deliver native event to Java");
        }
    }

    if (package != nullptr) env->DeleteLocalRef(package);
    if (status_value != nullptr) env->DeleteLocalRef(status_value);
    if (message_value != nullptr) env->DeleteLocalRef(message_value);
    if (attached_here) g_vm->DetachCurrentThread();
}

} // namespace ufu
