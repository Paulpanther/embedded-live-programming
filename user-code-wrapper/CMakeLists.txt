cmake_minimum_required(VERSION 3.24)
project(user_code)

set(CMAKE_CXX_STANDARD 17)
set(LIB_NAME "code" CACHE STRING "Name of lib file")

file(GLOB user_src "src/user/*.h" "src/user/*.cpp")

add_library(user_code SHARED src/interface.cc src/types.cc src/utils.cc src/code.cc src/arduino.cc ${user_src})
#add_executable(user_code src/code.cpp src/interface.cc src/serial.cc src/types.cc src/utils.cc)

set_target_properties(user_code PROPERTIES LIBRARY_OUTPUT_NAME ${LIB_NAME})
target_include_directories(user_code PUBLIC include)
