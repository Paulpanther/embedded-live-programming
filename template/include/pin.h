#pragma once

#include "code.h"
#include <iostream>

class InputPin {
	int pin;
public:
	explicit InputPin(int pin, int mode = INPUT): pin(pin) {
		pinMode(pin, mode);
	}

	int digitalRead() const {
		return ::digitalRead(pin);
	}

	int analogRead() const {
		return ::analogRead(pin);
	}
};

class EncoderPin {
    int pin;
    int channel;
public:
    EncoderPin(int pin, int channel): pin(pin), channel(channel) {
        attachCounter(channel, pin);
        clear();
    }

    int getDistance() { return read(); }

    int read() {
        return readCounter(channel);
    }

    void clear() {
        clearCounter(channel);
    }
};

int lastChannel = 0;

class OutputPin {
	int pin;
    int channel;
    int off;
public:
	explicit OutputPin(int pin, int off = LOW): pin(pin), off(off) {
		pinMode(pin, OUTPUT);
        channel = lastChannel++;
        ledcSetup(channel, 8, 5000);
        ledcAttachPin(pin, channel);
	}

    ~OutputPin() {
        digitalWrite(off);
    }

	void digitalWrite(int value) const {
		analogWrite(value * 255);
	}

    void analogWrite(float value) const {
        ledcWrite(channel, value);
    }
};
