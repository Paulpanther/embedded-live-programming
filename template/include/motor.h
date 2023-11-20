#pragma once

#include "pin.h"

struct Motor {
    OutputPin drivePin, forwardPin, backwardPin;

    Motor(OutputPin drivePin, OutputPin forwardPin, OutputPin backwardPin)
    : drivePin(drivePin), forwardPin(forwardPin), backwardPin(backwardPin) {}

    void drive(float speed) {
        forwardPin.digitalWrite(speed > 0);
        backwardPin.digitalWrite(speed <= 0);
        drivePin.analogWrite(abs((int) (speed * 255)));
    }

    void stop() {
        drive(0);
    }
};
