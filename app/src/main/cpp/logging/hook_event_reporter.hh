#ifndef HOOK_EVENT_REPORTER_HEADER
#define HOOK_EVENT_REPORTER_HEADER

#include <jni.h>

namespace HookEventReporter {
bool Initialize(JNIEnv* env, jclass module_class);
void Report(const char* package_name, const char* status, const char* message);
} // namespace HookEventReporter

#endif // HOOK_EVENT_REPORTER_HEADER
