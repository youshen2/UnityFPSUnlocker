#include "fpslimiter.hh"

#include <chrono>
#include <cstdio>
#include <string>
#include <thread>

#include "logging/hook_event_reporter.hh"
#include "unity/unity_engine.hh"
#include "utility/logger.hh"

#include <xdl.h>

namespace FPSLimiter {
    void Start(const ConfigValue& cfg) {
        const bool modify_frame_rate = cfg.fps_ > 0;
        const bool modify_resolution = cfg.scale_ > 0;
        if (!modify_frame_rate && !modify_resolution) {
            LOG("No native parameters selected for modification; skipping initialization");
            return;
        }

#ifdef __aarch64__
        LOG("[UnityFPSUnlocker][arm64] Starting...");
#elif defined(__ARM_ARCH_7A__)
        LOG("[UnityFPSUnlocker][armv7] Starting...");
#elif defined(__i386__)
        LOG("[UnityFPSUnlocker][x86] Starting...");
#elif defined(__x86_64__)
        LOG("[UnityFPSUnlocker][x86_64] Starting...");
#endif
        cfg.DebugPrint();
        std::chrono::seconds sleep_duration(cfg.delay_);
        std::this_thread::sleep_for(sleep_duration);

        LOG("***** begin *****");
        void* handle = nullptr;
        for (int i = 0; i < 10; ++i) {
            if ((handle = xdl_open("libil2cpp.so", 0))) {
                break;
            }
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }
        if (handle) {
            Unity::GetInstance().Init(handle, modify_frame_rate, modify_resolution);
            bool succeeded = true;
            if (modify_frame_rate) {
                succeeded = Unity::GetInstance().SetFrameRate(cfg.fps_, cfg.mod_opcode_)
                    && succeeded;
            }
            if (modify_resolution) {
                succeeded = Unity::GetInstance().SetResolution(cfg.scale_)
                    && succeeded;
            }

            if (succeeded) {
                char message[128];
                std::snprintf(
                    message,
                    sizeof(message),
                    "fps=%d resolution_scale=%.3f",
                    modify_frame_rate ? cfg.fps_ : -1,
                    modify_resolution ? cfg.scale_ : -1.0f);
                HookEventReporter::Report(
                    cfg.package_name_.c_str(),
                    "success",
                    message);
            }
            else {
                HookEventReporter::Report(
                    cfg.package_name_.c_str(),
                    "failed",
                    "reason=unity_icall_unavailable");
            }
            return;
        }
        else {
            HookEventReporter::Report(
                cfg.package_name_.c_str(),
                "failed",
                "reason=libil2cpp_not_found");
        }
    }
} // namespace FPSLimiter
