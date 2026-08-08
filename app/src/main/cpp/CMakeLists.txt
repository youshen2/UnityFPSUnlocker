cmake_minimum_required(VERSION 3.22.1)

project(UnityFPSUnlocker LANGUAGES C CXX)

set(CMAKE_C_STANDARD 17)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    xdl
    STATIC
    third/xdl/xdl.c
    third/xdl/xdl_iterate.c
    third/xdl/xdl_linker.c
    third/xdl/xdl_lzma.c
    third/xdl/xdl_util.c
)

target_include_directories(xdl PUBLIC third/xdl)
target_compile_options(xdl PRIVATE -Oz -ffunction-sections -fdata-sections)

add_library(
    UnityFPSUnlocker
    SHARED
    xposed_entry.cc
    fpslimiter.cc
    logging/hook_event_reporter.cc
    unity/unity_engine.cc
    utility/config_lite.cc
)

target_include_directories(
    UnityFPSUnlocker
    PRIVATE
    .
    third/xdl
)

target_compile_options(
    UnityFPSUnlocker
    PRIVATE
    -fno-rtti
    -fno-exceptions
    -ffunction-sections
    -fdata-sections
)

target_link_options(
    UnityFPSUnlocker
    PRIVATE
    -Wl,--exclude-libs,ALL
    -Wl,--gc-sections
)

find_library(log-lib log)
find_library(dl-lib dl)

target_link_libraries(
    UnityFPSUnlocker
    PRIVATE
    xdl
    ${log-lib}
    ${dl-lib}
)

set_target_properties(
    UnityFPSUnlocker
    PROPERTIES
    C_VISIBILITY_PRESET hidden
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN YES
)
