#pragma once

#include "State.h"
#include "HomeState.h"
#include "pin.h"
#include "motors.h"

struct ReturnState: public State {
    EncoderPin encoder = EncoderPin(18, 0);
    Motors motors = Motors();

    int distance = 0;
    ReturnState() = default;
    explicit ReturnState(int distance): distance(distance) {}

    State * loop() override {
        // Drive back to given distance, then return new HomeState()

        return nullptr;
    }
};
