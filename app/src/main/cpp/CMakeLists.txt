cmake_minimum_required(VERSION 3.22)
project(nayllm C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    llama
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG        v0.9.0
    GIT_SHALLOW    TRUE
)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS ON CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ARM_ARCH "armv8.6-a+dotprod+i8mm" CACHE STRING "" FORCE)

FetchContent_MakeAvailable(llama)

# Compila a JNI e linka contra o alvo 'llama' completo, que ja contem
# llama.cpp, common/, tools/mtmd (clip, audio) e os symbols necessarios.
add_library(nayllm SHARED llm_jni.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/mtmd.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/mtmd-helper.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/clip.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/mtmd-image.cpp)
target_link_libraries(nayllm PRIVATE llama)
target_include_directories(nayllm PRIVATE
    ${llama_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/ggml/include
    ${llama_SOURCE_DIR}/tools/mtmd
    ${llama_SOURCE_DIR}/common
)
target_compile_options(nayllm PRIVATE -O3 -fvisibility=hidden)
target_link_libraries(nayllm PRIVATE android log)
