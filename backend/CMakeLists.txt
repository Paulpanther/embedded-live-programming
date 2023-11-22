cmake_minimum_required(VERSION 3.24)
project(runner)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

set(CMAKE_CXX_STANDARD 17)

add_library(runner SHARED src/runner.cpp)
#add_executable(embedded_live_programming_runner src/test.cpp src/arduino.cpp src/utils.cc src/types.cc src/serial.cc src/interface.cc)
target_include_directories(runner PUBLIC include)
install(TARGETS runner)
