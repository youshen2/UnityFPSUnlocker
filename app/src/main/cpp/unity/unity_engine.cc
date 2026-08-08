#include "unity_engine.hh"

#include <xdl.h>

#include "utility/config.hh"
#include "utility/logger.hh"

void Unity::Init(void* handle, bool modify_frame_rate, bool modify_resolution) {
    if ((il2cpp_resolve_icall = (il2cpp_resolve_icall_f)xdl_sym(handle, "il2cpp_resolve_icall", nullptr)) == nullptr) {
        ERROR("il2cpp_resolve_icall not found");
        return;
    }

    if (modify_frame_rate) {
        if ((set_targetFrameRate = (set_targetFrameRate_f)il2cpp_resolve_icall("UnityEngine.Application::set_targetFrameRate")) == nullptr) {
            ERROR("set_targetFrameRate not found");
        }
    }

    if (!modify_resolution) {
        return;
    }

    if ((SetResolution_Internal = (SetResolution_t)il2cpp_resolve_icall("UnityEngine.Screen::SetResolution")) == nullptr) {
        if ((SetResolution_Internal = (SetResolution_t)il2cpp_resolve_icall("UnityEngine.Screen::SetResolution_Injected")) == nullptr) {
            ERROR("SetResolution not found");

#if defined(__aarch64__)
            if (auto get_height_ptr = il2cpp_resolve_icall("UnityEngine.Screen::get_height")) {
                auto ptr = reinterpret_cast<uint32_t*>(get_height_ptr);
                auto opcode = ptr[1];
                if ((opcode & 0xFC000000) == 0x94000000) {
                    auto offset = (int32_t)(opcode << 8) >> 6;
                    auto addr = reinterpret_cast<intptr_t>(get_height_ptr) + 4 + offset;
                    auto GetScreenManager = reinterpret_cast<void* (*)(void)>(addr);
                    screen_manager_ = reinterpret_cast<ScreenManager*>(GetScreenManager());
                }
            }
#endif

            if (screen_manager_ == nullptr) {
                ERROR("ScreenManager not found");
            }
        }
    }

    if ((GetSystemExtImpl_Internal = (GetSystemExtImpl_t)il2cpp_resolve_icall("UnityEngine.Display::GetSystemExtImpl")) == nullptr) {
        ERROR("GetSystemExtImpl not found");
    }
}

Resolution Unity::GetSystemExtImpl() {
    Resolution resolution;
    if (GetSystemExtImpl_Internal) {
        GetSystemExtImpl_Internal(nullptr, &resolution.m_Width, &resolution.m_Height);
    }
    return resolution;
}

bool Unity::SetResolution(float scale) {
    if (scale <= 0) {
        return false;
    }

    Resolution resolution = GetSystemExtImpl();
    if (resolution.m_Width > 0 && resolution.m_Height > 0) {
        auto target_width = static_cast<int32_t>(resolution.m_Width * scale);
        auto target_height = static_cast<int32_t>(resolution.m_Height * scale);
        int preferred_refresh_rate = 0;

        if (SetResolution_Internal) {
            LOG("Set resolution: %d x %d", target_width, target_height);
            SetResolution_Internal(target_width, target_height, 1, &preferred_refresh_rate);
            return Utility::NopFunc(
                reinterpret_cast<unsigned char*>(SetResolution_Internal));
        }
        else if (screen_manager_) {
            LOG("Set resolution: %d x %d", target_width, target_height);
            screen_manager_->RequestResolution(target_width, target_height, 1, &preferred_refresh_rate);
            return true;
        }
    }
    return false;
}

bool Unity::SetFrameRate(int framerate, bool mod_opcode) {
    if (framerate <= 0) {
        return false;
    }

    if (set_targetFrameRate) {
        set_targetFrameRate(framerate);
        LOG("Set target frame rate: %d", framerate);

        if (mod_opcode) {
            auto ptr = reinterpret_cast<uint32_t*>(set_targetFrameRate);
#if defined(__aarch64__)
            auto opcode = ptr[0];
            if ((opcode & 0xFC000000) == 0x14000000) {
                auto offset = (int32_t)(opcode << 8) >> 6;
                ptr = reinterpret_cast<uint32_t*>(reinterpret_cast<intptr_t>(set_targetFrameRate) + offset);
            }
#endif
            if (!Utility::NopFunc(reinterpret_cast<unsigned char*>(ptr))) {
                return false;
            }
        }
        return true;
    }
    return false;
}
