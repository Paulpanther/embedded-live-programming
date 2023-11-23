#pragma once

#include "State.h"
#include "ReturnState.h"
#include "code.h"

struct ButtonState: public State {
    int distance = 0;
    InputPin button = InputPin(13, INPUT_PULLUP);

    ButtonState() = default;
    explicit ButtonState(int distance): distance(distance) {}

    State * loop() override {
        // Read button with button.digitalRead()
        // When button is pressed return new ReturnState()
        return nullptr;
    }
};
