#pragma once

#include <string>

extern "C" struct Probe {
    int code;
    std::string value;
    std::string type;
};

