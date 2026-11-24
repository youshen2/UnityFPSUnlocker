#include "il2cpp_runtime.hh"

#include <chrono>
#include <thread>

#include <xdl.h>

#include "log.hh"
#include "memory_patch.hh"

namespace ufu {

bool Il2CppRuntime::Initialize(void* handle, bool need_frame_rate, bool need_resolution) {
    need_frame_rate_ = need_frame_rate;
    need_resolution_ = need_resolution;

    resolve_icall_ = reinterpret_cast<ResolveIcallFn>(
        xdl_sym(handle, "il2cpp_resolve_icall", nullptr));
    if (resolve_icall_ == nullptr) {
        UFU_ERR("il2cpp_resolve_icall not found");
        return false;
    }

    if (need_frame_rate_) {
        set_target_frame_rate_ = reinterpret_cast<SetTargetFrameRateFn>(
            resolve_icall_("UnityEngine.Application::set_targetFrameRate"));
        get_target_frame_rate_ = reinterpret_cast<GetTargetFrameRateFn>(
            resolve_icall_("UnityEngine.Application::get_targetFrameRate"));
        set_vsync_count_ = reinterpret_cast<SetVSyncCountFn>(
            resolve_icall_("UnityEngine.QualitySettings::set_vSyncCount"));
        get_vsync_count_ = reinterpret_cast<GetVSyncCountFn>(
            resolve_icall_("UnityEngine.QualitySettings::get_vSyncCount"));
        get_frame_count_ = reinterpret_cast<GetFrameCountFn>(
            resolve_icall_("UnityEngine.Time::get_frameCount"));
        get_realtime_ = reinterpret_cast<GetRealtimeFn>(
            resolve_icall_("UnityEngine.Time::get_realtimeSinceStartup"));

        if (set_target_frame_rate_ == nullptr) {
            UFU_ERR("set_targetFrameRate not found");
        }
        if (get_target_frame_rate_ == nullptr) {
            UFU_LOG("get_targetFrameRate not found; skip verification");
        }
    }

    if (!need_resolution_) {
        return true;
    }

    set_resolution_ = reinterpret_cast<SetResolutionFn>(
        resolve_icall_("UnityEngine.Screen::SetResolution"));
    if (set_resolution_ == nullptr) {
        set_resolution_ = reinterpret_cast<SetResolutionFn>(
            resolve_icall_("UnityEngine.Screen::SetResolution_Injected"));
    }

#if defined(__aarch64__)
    if (set_resolution_ == nullptr) {
        if (auto* get_height = resolve_icall_("UnityEngine.Screen::get_height")) {
            auto* instructions = reinterpret_cast<uint32_t*>(get_height);
            const auto opcode = instructions[1];
            if ((opcode & 0xFC000000U) == 0x94000000U) {
                const auto offset = static_cast<int32_t>(opcode << 8) >> 6;
                const auto target = reinterpret_cast<intptr_t>(get_height) + 4 + offset;
                auto get_screen_manager = reinterpret_cast<void* (*)(void)>(target);
                screen_manager_ = reinterpret_cast<ScreenManager*>(get_screen_manager());
            }
        }
    }
#endif

    if (set_resolution_ == nullptr && screen_manager_ == nullptr) {
        UFU_ERR("SetResolution not found");
    }

    get_system_ext_impl_ = reinterpret_cast<GetSystemExtImplFn>(
        resolve_icall_("UnityEngine.Display::GetSystemExtImpl"));
    if (get_system_ext_impl_ == nullptr) {
        UFU_ERR("GetSystemExtImpl not found");
    }

    // ????????? icall ??????????????????
    return true;
}

Resolution Il2CppRuntime::ReadResolution() {
    Resolution resolution;
    if (get_system_ext_impl_ != nullptr) {
        get_system_ext_impl_(nullptr, &resolution.width, &resolution.height);
    }
    return resolution;
}

bool Il2CppRuntime::SetTargetFrameRate(int fps, bool patch_opcode) {
    if (set_target_frame_rate_ == nullptr || fps <= 0) return false;

    // 部分游戏用垂直同步压住帧率；锁定帧率时同时关闭并锁住 vSyncCount。
    if (patch_opcode && set_vsync_count_ != nullptr) {
        set_vsync_count_(0);
        auto* vsync_target = FollowFirstBranch(
            reinterpret_cast<unsigned char*>(set_vsync_count_));
        if (!PatchToReturn(vsync_target)) {
            UFU_ERR("Failed to patch set_vSyncCount");
        }
    }

    set_target_frame_rate_(fps);
    UFU_LOG("Target frame rate set to %d", fps);

    if (patch_opcode) {
        auto* target = reinterpret_cast<unsigned char*>(set_target_frame_rate_);
        target = FollowFirstBranch(target);
        if (!PatchToReturn(target)) {
            UFU_ERR("Failed to patch set_targetFrameRate");
            return false;
        }
    }

    // 回读目标帧率，确认设置没有被引擎或游戏立即改回。
    if (get_target_frame_rate_ != nullptr) {
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        const int actual = get_target_frame_rate_();
        UFU_LOG("targetFrameRate readback: %d (expected %d)", actual, fps);
        if (actual != fps) return false;
    }

    if (get_vsync_count_ != nullptr) {
        UFU_LOG("vSyncCount readback: %d", get_vsync_count_());
    }

    return true;
}

float Il2CppRuntime::MeasureFps(int seconds) {
    if (get_frame_count_ == nullptr || get_realtime_ == nullptr || seconds <= 0) {
        return -1.0f;
    }

    const int start_frames = get_frame_count_();
    const float start_time = get_realtime_();
    std::this_thread::sleep_for(std::chrono::seconds(seconds));
    const int end_frames = get_frame_count_();
    const float end_time = get_realtime_();

    const float elapsed = end_time - start_time;
    if (elapsed <= 0.0f) return -1.0f;
    return static_cast<float>(end_frames - start_frames) / elapsed;
}

bool Il2CppRuntime::SetRenderScale(float scale) {
    if (scale <= 0.0f) return false;

    if (set_resolution_ == nullptr && screen_manager_ == nullptr) {
        UFU_ERR("No resolution API available");
        return false;
    }

    const Resolution resolution = ReadResolution();
    if (resolution.width <= 0 || resolution.height <= 0) {
        UFU_ERR("Invalid display resolution");
        return false;
    }

    const auto target_width = static_cast<int32_t>(resolution.width * scale);
    const auto target_height = static_cast<int32_t>(resolution.height * scale);
    if (target_width <= 0 || target_height <= 0) return false;

    int preferred_refresh_rate = 0;
    UFU_LOG("Set resolution: %d x %d", target_width, target_height);

    if (set_resolution_ != nullptr) {
        set_resolution_(target_width, target_height, 1, &preferred_refresh_rate);
        return PatchToReturn(reinterpret_cast<unsigned char*>(set_resolution_));
    }

    screen_manager_->RequestResolution(
        target_width,
        target_height,
        1,
        &preferred_refresh_rate);
    return true;
}

} // namespace ufu
