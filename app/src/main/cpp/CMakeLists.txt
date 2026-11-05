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

# Compila todos os fontes do llama.cpp + mtmd + common + vendor em um unico
# .so, linkando apenas contra ggml. Evita depender do alvo 'llama' (que traz
# httplib/llama_build_info) e do alvo 'mtmd' (inexistente).
file(GLOB_RECURSE LLAMA_CPP ${llama_SOURCE_DIR}/src/*.cpp)
file(GLOB_RECURSE MTMD_CPP ${llama_SOURCE_DIR}/tools/mtmd/*.cpp)
list(REMOVE_ITEM MTMD_CPP
    ${llama_SOURCE_DIR}/tools/mtmd/debug/mtmd-debug.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/deprecation-warning.cpp
    ${llama_SOURCE_DIR}/tools/mtmd/mtmd-cli.cpp)
file(GLOB COMMON_CPP ${llama_SOURCE_DIR}/common/*.cpp ${llama_SOURCE_DIR}/common/*.c)
file(GLOB VENDOR_HASH_CPP ${llama_SOURCE_DIR}/vendor/hash/*.cpp ${llama_SOURCE_DIR}/vendor/hash/*.c ${llama_SOURCE_DIR}/vendor/hash/sha256/*.c)

add_library(nayllm SHARED
    llm_jni.cpp
    ${LLAMA_CPP}
    ${MTMD_CPP}
    ${COMMON_CPP}
    ${VENDOR_HASH_CPP}
)

target_include_directories(nayllm PRIVATE
    ${llama_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/ggml/include
    ${llama_SOURCE_DIR}/src
    ${CMAKE_CURRENT_BINARY_DIR}/src
    ${llama_SOURCE_DIR}/tools/mtmd
    ${llama_SOURCE_DIR}/common
    ${llama_SOURCE_DIR}/vendor
    ${llama_SOURCE_DIR}/vendor/hash
    ${llama_SOURCE_DIR}/vendor/hash/sha256
    ${llama_SOURCE_DIR}/vendor/hash/rotate-bits
)
target_compile_options(nayllm PRIVATE -O3 -fvisibility=hidden)
target_link_libraries(nayllm PRIVATE ggml android log)
