cmake_minimum_required(VERSION 3.22)
project(nayllm C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp fixado em uma release publicada (v0.9.0). Atualizar apenas com revisão da API mtmd.
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
set(LLAMA_BUILD_TOOLS ON CACHE BOOL "" FORCE)   # necessário para o alvo mtmd (visão)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)        # threads próprias do ggml; sem OpenMP no NDK
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)        # cross-compile para o ABI alvo, sem -march do host

# Otimização para o alvo: dotprod + i8mm são o que acelera os kernels Q4_K/Q4_0 em ARMv8.6+
# (Snapdragon 8 Elite e similares). Dispositivos sem esses recursos caem no caminho genérico.
set(GGML_CPU_ARM_ARCH "armv8.6-a+dotprod+i8mm" CACHE STRING "" FORCE)

FetchContent_MakeAvailable(llama)

add_library(nayllm SHARED llm_jni.cpp)
target_link_libraries(nayllm PRIVATE llama ggml mtmd)
target_include_directories(nayllm PRIVATE
    ${llama_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/ggml/include
    ${llama_SOURCE_DIR}/tools/mtmd
)
target_compile_options(nayllm PRIVATE -O3 -fvisibility=hidden)
