#pragma once

#include <vector>
#include <string>
#include "arduino.h"
#include <iostream>

extern "C" struct Probe {
    int code;
    std::string value;
    std::string type;
};

// Will be called by probes in user code
int add_probe(int code, int value);
float add_probe(int code, float value);
double add_probe(int code, double value);
bool add_probe(int code, bool value);
const char* add_probe(int code, const char* value);

template<typename T>
T& add_probe(int code, T& value);

// Will be defined by user code
void setup();
void loop();

// Lib functions that runner will call
extern "C" void _setup(Interface*);
extern "C" void _loop();
extern "C" void onClose();
