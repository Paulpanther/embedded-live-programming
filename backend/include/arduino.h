#pragma once

#include <thread>
#include <iostream>
#include <termios.h>
#include <fcntl.h>
#include <cstring>
#include <cmath>
#include "interface.tcc"

using namespace std::chrono_literals;

void initSerial();
void closeSerial();
Interface* getInterface();
