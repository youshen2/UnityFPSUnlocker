#pragma once

#include <stdint.h>

namespace ufu {

struct Resolution {
    int32_t width = 0;
    int32_t height = 0;
    int32_t refresh_rate = 0;
};

class ScreenManager {
public:
    virtual ~ScreenManager() {}
    virtual void RequestResolution(int, int, int, void*);
};

/**
 * 通过 Unity 导出�?il2cpp_resolve_icall 访问引擎内部 API�? * 所有函数指针在 Initialize 阶段解枝，之坎坯跨线程使用�? */
class Il2CppRuntime {
public:
    bool Initialize(void* il2cpp_handle, bool need_frame_rate, bool need_resolution);

    bool SetTargetFrameRate(int fps, bool patch_opcode);
    bool SetRenderScale(float scale);

    /** ????????????? Unity Time icall ??? -1? */
    float MeasureFps(int seconds);

private:
    using ResolveIcallFn = void* (*)(const char*);
    using SetTargetFrameRateFn = void (*)(int);
    using GetTargetFrameRateFn = int (*)();
    using SetVSyncCountFn = void (*)(int);
    using GetVSyncCountFn = int (*)();
    using GetFrameCountFn = int (*)();
    using GetRealtimeFn = float (*)();
    using SetResolutionFn = void (*)(int, int, int, void*);
    using GetSystemExtImplFn = void (*)(void*, int*, int*);

    Resolution ReadResolution();

    ResolveIcallFn resolve_icall_ = nullptr;
    SetTargetFrameRateFn set_target_frame_rate_ = nullptr;
    GetTargetFrameRateFn get_target_frame_rate_ = nullptr;
    SetVSyncCountFn set_vsync_count_ = nullptr;
    GetVSyncCountFn get_vsync_count_ = nullptr;
    GetFrameCountFn get_frame_count_ = nullptr;
    GetRealtimeFn get_realtime_ = nullptr;
    SetResolutionFn set_resolution_ = nullptr;
    GetSystemExtImplFn get_system_ext_impl_ = nullptr;
    ScreenManager* screen_manager_ = nullptr;

    bool need_frame_rate_ = false;
    bool need_resolution_ = false;
};

} // namespace ufu
