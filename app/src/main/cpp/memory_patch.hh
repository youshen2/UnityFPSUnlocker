#pragma once

namespace ufu {

/** 把目标函数首指令改写为直接返回（等效 NOP 掉函数体）。 */
bool PatchToReturn(unsigned char* address);

/** 跟随 ARM64 的单条 B 跳转，取得实际函数入口。 */
unsigned char* FollowFirstBranch(unsigned char* address);

} // namespace ufu
