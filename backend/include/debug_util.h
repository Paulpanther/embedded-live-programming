#pragma once

#include <iostream>

bool logTimeEnabled = false;

void logTime(std::string msg) {
    if (!logTimeEnabled) return;

    time_t now = time(0);
    struct tm tstruct = *localtime(&now);
    char buf[80];
    strftime(buf, sizeof(buf), "%X", &tstruct);

    std::cout << msg << ": " << buf << std::endl;
}
