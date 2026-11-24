#pragma once

#include "config.hh"

namespace ufu {

/** 在独立线程中等待 libil2cpp 加载，然后设置帧率与渲染分辨率。 */
void Run(const Config& config);

} // namespace ufu
