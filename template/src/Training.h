#pragma once

#include "pin.h"
#include "motors.h"

struct Training {
    int threshold = 0;
    InputPin potti = InputPin(35);
    OutputPin led = OutputPin(12);

    void loop() {
        // Show led when potti is rotated over half
    }
};
