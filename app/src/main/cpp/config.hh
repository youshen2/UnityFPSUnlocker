#pragma once

#include <string>
#include <utility>

namespace ufu {

struct Config {
    std::string package_name;
    int delay = 5;
    int fps = -1;
    bool modify_opcode = true;
    float scale = -1.0f;

    Config() = default;

    Config(std::string package, int delay_seconds, int target_fps, bool patch_opcode, float render_scale)
        : package_name(std::move(package)),
          delay(delay_seconds),
          fps(target_fps),
          modify_opcode(patch_opcode),
          scale(render_scale) {}

    bool WantsFrameRate() const { return fps > 0; }
    bool WantsResolution() const { return scale > 0.0f; }
};

} // namespace ufu
