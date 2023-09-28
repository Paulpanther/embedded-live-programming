#pragma once

struct State {
    virtual State* loop() = 0;
};