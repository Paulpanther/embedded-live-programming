#pragma once

#include <string>
#include "pin.h"
#include "State.h"
#include "HomeState.h"
#include "DriveState.h"

struct Main {
    State* state = new DriveState();

	void loop() {
        State* next = state->loop();
        if (next != nullptr) {
            state = next;
        }
	}
};
