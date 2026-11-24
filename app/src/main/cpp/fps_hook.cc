#include "fps_hook.hh"

#include <chrono>
#include <cstdio>
#include <thread>

#include <xdl.h>

#include "event_reporter.hh"
#include "il2cpp_runtime.hh"
#include "log.hh"

namespace ufu {
namespace {

void* OpenIl2Cpp() {
    for (int attempt = 0; attempt < 10; ++attempt) {
        if (void* handle = xdl_open("libil2cpp.so", 0)) return handle;
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    return nullptr;
}

} // namespace

void Run(const Config& config) {
    if (!config.WantsFrameRate() && !config.WantsResolution()) {
        UFU_LOG("No native parameters enabled, skip");
        return;
    }

#if defined(__aarch64__)
    UFU_LOG("Starting on arm64-v8a");
#elif defined(__ARM_ARCH_7A__)
    UFU_LOG("Starting on armeabi-v7a");
#elif defined(__i386__)
    UFU_LOG("Starting on x86");
#elif defined(__x86_64__)
    UFU_LOG("Starting on x86_64");
#endif

    if (config.delay > 0) {
        std::this_thread::sleep_for(std::chrono::seconds(config.delay));
    }

    void* handle = OpenIl2Cpp();
    if (handle == nullptr) {
        UFU_ERR("libil2cpp.so not found");
        ReportEvent(
            config.package_name.c_str(),
            "failed",
            "reason=libil2cpp_not_found");
        return;
    }

    Il2CppRuntime runtime;
    if (!runtime.Initialize(handle, config.WantsFrameRate(), config.WantsResolution())) {
        ReportEvent(
            config.package_name.c_str(),
            "failed",
            "reason=unity_icall_unavailable");
        return;
    }

    bool success = true;
    if (config.WantsFrameRate()) {
        success = runtime.SetTargetFrameRate(config.fps, config.modify_opcode) && success;
    }
    if (config.WantsResolution()) {
        success = runtime.SetRenderScale(config.scale) && success;
    }

    if (!success) {
        ReportEvent(
            config.package_name.c_str(),
            "failed",
            "reason=unity_icall_unavailable");
        return;
    }

    float measured_fps = -1.0f;
    if (config.WantsFrameRate()) {
        measured_fps = runtime.MeasureFps(2);
    }

    char message[220];
    std::snprintf(
        message,
        sizeof(message),
        "fps=%d modify_opcode=%d measured_fps=%.1f resolution_scale=%.3f",
        config.WantsFrameRate() ? config.fps : -1,
        config.modify_opcode ? 1 : 0,
        measured_fps,
        config.WantsResolution() ? config.scale : -1.0f);
    ReportEvent(config.package_name.c_str(), "success", message);
}

} // namespace ufu
