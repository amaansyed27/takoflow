cmake_minimum_required(VERSION 3.22.1)
project(takoflow_whisper LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android Studio's debug APK previously built whisper.cpp with -O0. Native
# inference is the hot path, so keep symbols while using release-grade
# optimization for local device testing.
if(ANDROID)
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O3 -DNDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O3 -DNDEBUG")
endif()

include(FetchContent)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    whispercpp
    URL https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.8.1.tar.gz
)
FetchContent_MakeAvailable(whispercpp)

add_library(takoflow_whisper SHARED whisper_jni.cpp)
target_include_directories(takoflow_whisper PRIVATE ${whispercpp_SOURCE_DIR}/include)
target_link_libraries(takoflow_whisper PRIVATE whisper android log)
