#pragma once

#include <jni.h>

namespace ufu {

/** 把 native Hook 结果回传到 NativeBridge.reportNativeEvent。 */
bool InitializeEventReporter(JNIEnv* env, jclass bridge_class);
void ReportEvent(const char* package_name, const char* status, const char* message);

} // namespace ufu
