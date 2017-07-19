cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

add_library(image_stitching SHARED src/main/cpp/image_stitching.cpp )
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so")

include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include/opencv)

find_library(log-lib log )

add_subdirectory(${CMAKE_SOURCE_DIR}/src/main/cpp/utils)

target_link_libraries(image_stitching ${log-lib} -ljnigraphics lib_opencv jniUtils)