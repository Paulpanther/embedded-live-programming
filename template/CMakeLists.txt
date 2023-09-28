cmake_minimum_required(VERSION 3.21)
project(my_project)
set(CMAKE_CXX_STANDARD 17)

file(GLOB user_src "src/*.h" "src/*.cpp")
file(GLOB example_src "examples/*.h" "examples/*.cpp")

add_executable(my_project ${user_src} ${example_src})
target_include_directories(my_project PUBLIC include src)
