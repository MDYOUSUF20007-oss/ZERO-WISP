cmake_minimum_required(VERSION 3.22.1)
project(zerowisp_native CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ARM64 NEON optimizations
set(COMMON_FLAGS "-O3 -DNDEBUG -ffast-math")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${COMMON_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMMON_FLAGS}")

set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/whisper.cpp)

# Check if whisper.cpp has been cloned
if(EXISTS ${WHISPER_DIR}/whisper.h)
    message(STATUS "whisper.cpp found, building native engine")

    # Whisper.cpp definitions for Android
    add_compile_definitions(
        GGML_USE_CPU
        GGML_USE_CPU_HBM=0
        WHISPER_NO_AVX
        WHISPER_NO_AVX2
        WHISPER_NO_F16C
        WHISPER_NO_FMA
        _GNU_SOURCE
    )

    set(GGML_SOURCES
        ${WHISPER_DIR}/ggml/src/ggml.c
        ${WHISPER_DIR}/ggml/src/ggml-alloc.c
        ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
        ${WHISPER_DIR}/ggml/src/ggml-threading.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.c
    )

    # Optional AARCH64-specific file
    if(EXISTS ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp)
        list(APPEND GGML_SOURCES ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp)
    endif()
    if(EXISTS ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.c)
        list(APPEND GGML_SOURCES ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.c)
    endif()

    set(WHISPER_SOURCES
        ${WHISPER_DIR}/whisper.cpp
        ${GGML_SOURCES}
    )

    add_library(zerowisp_jni SHARED
        zerowisp_jni.cpp
        ${WHISPER_SOURCES}
    )

    target_include_directories(zerowisp_jni PRIVATE
        ${WHISPER_DIR}
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}/ggml/include
        ${WHISPER_DIR}/ggml/src
        ${WHISPER_DIR}/ggml/src/ggml-cpu
    )

    target_compile_options(zerowisp_jni PRIVATE
        -O3
        -ffast-math
        -DNDEBUG
    )

else()
    message(WARNING "whisper.cpp not found at ${WHISPER_DIR} — building stub JNI only")

    add_library(zerowisp_jni SHARED
        zerowisp_jni.cpp
    )

    # Stub definitions so the file compiles without whisper headers
    target_compile_definitions(zerowisp_jni PRIVATE WHISPER_STUB_BUILD)
endif()

# Android log library
find_library(log-lib log)
target_link_libraries(zerowisp_jni ${log-lib})
