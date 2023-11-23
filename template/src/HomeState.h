#pragma once

#include "State.h"

struct HomeState: public State {
    State * loop() override {
        return nullptr;
    }
};