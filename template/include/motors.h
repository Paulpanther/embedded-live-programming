#pragma once

#include "motor.h"

struct Motors {
    Motor m1 = Motor(OutputPin(25), OutputPin(32), OutputPin(33));
    Motor m2 = Motor(OutputPin(26), OutputPin(27), OutputPin(14));

    void drive(float speed) {
        m1.drive(speed * 0.7);
        m2.drive(speed);
    }

    void stop() {
        m1.stop();
        m2.stop();
    }
};
