#include "memory_patch.hh"

#include <stdint.h>
#include <sys/mman.h>
#include <unistd.h>

#include "log.hh"

namespace ufu {
namespace {

bool Protect(void* pointer, size_t size, int protection) {
    const auto page_size = static_cast<uintptr_t>(getpagesize());
    const auto address = reinterpret_cast<uintptr_t>(pointer);
    const auto page_start = address & ~(page_size - 1);
    const auto page_end = (address + size + page_size - 1) & ~(page_size - 1);
    return mprotect(
               reinterpret_cast<void*>(page_start),
               page_end - page_start,
               protection) == 0;
}

} // namespace

bool PatchToReturn(unsigned char* address) {
    if (address == nullptr) return false;

#if defined(__arm__) && !defined(__aarch64__)
    const bool thumb = (reinterpret_cast<uintptr_t>(address) & 1U) != 0;
    if (thumb) --address;
#endif

    if (!Protect(address, 4, PROT_READ | PROT_WRITE | PROT_EXEC)) {
        UFU_ERR("mprotect failed for %p", address);
        return false;
    }

#if defined(__aarch64__)
    address[0] = 0xC0;
    address[1] = 0x03;
    address[2] = 0x5F;
    address[3] = 0xD6; // ret
#elif defined(__arm__)
    if (thumb) {
        address[0] = 0x70;
        address[1] = 0x47; // bx lr
        address[2] = 0x00;
        address[3] = 0xBF; // nop
    } else {
        address[0] = 0x1E;
        address[1] = 0xFF;
        address[2] = 0x2F;
        address[3] = 0xE1; // bx lr
    }
#elif defined(__i386__) || defined(__x86_64__)
    address[0] = 0xC3; // ret
#endif

    __builtin___clear_cache(
        reinterpret_cast<char*>(address),
        reinterpret_cast<char*>(address + 4));

    if (!Protect(address, 4, PROT_READ | PROT_EXEC)) {
        UFU_ERR("Failed to restore permissions for %p", address);
        return false;
    }
    return true;
}

unsigned char* FollowFirstBranch(unsigned char* address) {
    if (address == nullptr) return nullptr;

#if defined(__aarch64__)
    const auto instruction = *reinterpret_cast<uint32_t*>(address);
    if ((instruction & 0xFC000000U) == 0x14000000U) {
        const auto offset = static_cast<int32_t>(instruction << 8) >> 6;
        return address + offset;
    }
#endif
    return address;
}

} // namespace ufu
