#pragma once

#include "State.h"
#include "ButtonState.h"
#include "pin.h"
#include "motors.h"

struct DriveState: public State {
    EncoderPin encoder = EncoderPin(18, 0);
    InputPin light = InputPin(15);
    Motors motors = Motors();

    State * loop() override {
        // Drive motors (slow)
        // When light.analogRead() is dark stop the motors, delay a second, read the encoder and return a new ButtonState

        return nullptr;
    }
};
