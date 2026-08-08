#include "config.hh"

#include <stdint.h>
#include <unistd.h>

#include "logger.hh"

void ConfigValue::DebugPrint() const {
    LOG(
        "\tpackage: %s | delay: %d | fps: %d | mod_opcode: %d | scale: %f",
        package_name_.c_str(),
        delay_,
        fps_,
        mod_opcode_,
        scale_);
}

namespace Utility {
int ChangeMemPermission(void* pointer, size_t size, int permission) {
    const auto page_size = static_cast<uintptr_t>(getpagesize());
    const auto address = reinterpret_cast<uintptr_t>(pointer);
    const auto page_start = address & ~(page_size - 1);
    const auto page_end = (address + size + page_size - 1) & ~(page_size - 1);
    return mprotect(
        reinterpret_cast<void*>(page_start),
        page_end - page_start,
        permission);
}

bool NopFunc(unsigned char* pointer) {
    if (pointer == nullptr) {
        return false;
    }

#if defined(__arm__) && !defined(__aarch64__)
    const bool thumb_mode = (reinterpret_cast<uintptr_t>(pointer) & 1U) != 0;
    if (thumb_mode) {
        --pointer;
    }
#endif

    if (ChangeMemPermission(pointer, 4) != 0) {
        ERROR("Failed to make target function writable");
        return false;
    }

#if defined(__aarch64__)
    pointer[0] = 0xC0;
    pointer[1] = 0x03;
    pointer[2] = 0x5F;
    pointer[3] = 0xD6;
#elif defined(__arm__)
    if (thumb_mode) {
        pointer[0] = 0x70;
        pointer[1] = 0x47;
        pointer[2] = 0x00;
        pointer[3] = 0xBF;
    } else {
        pointer[0] = 0x1E;
        pointer[1] = 0xFF;
        pointer[2] = 0x2F;
        pointer[3] = 0xE1;
    }
#elif defined(__i386__) || defined(__x86_64__)
    pointer[0] = 0xC3;
#endif

    __builtin___clear_cache(
        reinterpret_cast<char*>(pointer),
        reinterpret_cast<char*>(pointer + 4));
    if (ChangeMemPermission(pointer, 4, PROT_READ | PROT_EXEC) != 0) {
        ERROR("Failed to restore target function permissions");
        return false;
    }
    return true;
}
} // namespace Utility
