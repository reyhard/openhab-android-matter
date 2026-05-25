cmake_minimum_required(VERSION 3.22.1)

project(openhab_matter_chip_stub LANGUAGES CXX)

add_library(openhab_matter_chip SHARED openhab_matter_chip_stub.cpp)

target_compile_features(openhab_matter_chip PRIVATE cxx_std_17)
