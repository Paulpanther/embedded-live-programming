#include "code.h"
#include <sstream>
#include <iostream>

extern "C" {
    std::vector<Probe> probes;
}

int add_probe(int code, int value) {
    std::ostringstream ss;
    ss << std::fixed << value;
    probes.push_back({code, ss.str(), "int"});
    return value;
}

float add_probe(int code, float value) {
    std::ostringstream ss;
    ss << std::fixed << value;
    probes.push_back({code, ss.str(), "float"});
    return value;
}

double add_probe(int code, double value) {
    std::ostringstream ss;
    ss << std::fixed << value;
    probes.push_back({code, ss.str(), "double"});
    return value;
}

bool add_probe(int code, bool value) {
    probes.push_back({code, value ? "true" : "false", "bool"});
    return value;
}

const char* add_probe(int code, const char* value) {
    probes.push_back({code, value, "string"});
    return value;
}

template<typename T>
T &add_probe(int code, T &value) {
    probes.push_back({code, std::to_string(value), "object"});
    return value;
}

void _setup() {
    std::cout << "USER SETUP" << std::endl;
    initSerial();
    setup();
}

void _loop() {
    probes.clear();
    loop();
}

void onClose() {
    std::cout << "USER CLOSE" << std::endl;
    closeSerial();
}
