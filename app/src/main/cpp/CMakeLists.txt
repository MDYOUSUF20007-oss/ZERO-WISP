cmake_minimum_required(VERSION 3.22.1)
project(zerowisp_native CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for mobile CPU inference
set(COMMON_FLAGS "-O3 -DNDEBUG -ffast-math")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${COMMON_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMMON_FLAGS}")

set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/whisper.cpp)

find_library(log-lib log)

# Check if whisper.cpp has been cloned (full native engine build)
if(EXISTS ${WHISPER_DIR}/CMakeLists.txt)
    message(STATUS "whisper.cpp found at ${WHISPER_DIR} — building full native engine")

    set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build whisper as static" FORCE)
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "Disable whisper tests" FORCE)
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "Disable whisper examples" FORCE)
    set(WHISPER_BUILD_SERVER OFF CACHE BOOL "Disable whisper server" FORCE)
    set(GGML_OPENMP OFF CACHE BOOL "Disable OpenMP on Android" FORCE)

    add_subdirectory(${WHISPER_DIR} ${CMAKE_CURRENT_BINARY_DIR}/whisper_build EXCLUDE_FROM_ALL)

    add_library(zerowisp_jni SHARED
        zerowisp_jni.cpp
    )

    target_include_directories(zerowisp_jni PRIVATE
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}
    )

    target_link_libraries(zerowisp_jni
        whisper
        ${log-lib}
    )

else()
    message(WARNING "whisper.cpp not found at ${WHISPER_DIR} — building stub JNI only")

    add_library(zerowisp_jni SHARED
        zerowisp_jni.cpp
    )

    # Stub definitions so the file compiles cleanly without whisper headers
    target_compile_definitions(zerowisp_jni PRIVATE WHISPER_STUB_BUILD)
    target_link_libraries(zerowisp_jni ${log-lib})
endif()
