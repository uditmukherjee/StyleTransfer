cmake_minimum_required(VERSION 3.4.1)

# Configure build library name.
set(TARGET_NAME jniUtils)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -std=c++11")

add_library(${TARGET_NAME} SHARED time_profiler.cpp)

target_include_directories(${TARGET_NAME} PRIVATE
                           include)

target_link_libraries(${TARGET_NAME} android log)