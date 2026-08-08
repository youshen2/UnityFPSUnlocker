#ifndef CONFIG_HEADER
#define CONFIG_HEADER

#include <sys/mman.h>
#include <string>
#include <utility>

class ConfigValue {
public:
    std::string package_name_;
    int delay_ = 5;
    int fps_ = 90;
    bool mod_opcode_ = true;
    float scale_ = -1;

    ConfigValue() = default;

    ConfigValue(std::string package_name, int delay, int fps, bool mod_opcode, float scale)
        : package_name_(std::move(package_name)),
          delay_(delay),
          fps_(fps),
          mod_opcode_(mod_opcode),
          scale_(scale) {}

    void DebugPrint() const;
};

namespace Utility {
int ChangeMemPermission(
    void* pointer,
    size_t size,
    int permission = PROT_READ | PROT_WRITE | PROT_EXEC);
bool NopFunc(unsigned char* pointer);
} // namespace Utility

#endif // CONFIG_HEADER
