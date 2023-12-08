#pragma once

#include <thread>
#include <iostream>
#include <termios.h>
#include <fcntl.h>
#include <cstring>
#include <cmath>
#include "interface.tcc"

#define HIGH 1
#define LOW 0
#define INPUT 1
#define OUTPUT 2
#define INPUT_PULLUP 5

using namespace std::chrono_literals;

void setInterface(Interface*);

// Following functions are provided for user-code

#define HIGH 1
#define LOW 0
#define INPUT 1
#define OUTPUT 2
#define INPUT_PULLUP 5
typedef uint8_t byte;

int analogRead(int pin);
int digitalRead(int pin);
void digitalWrite(int pin, int value);
void pinMode(int pin, int mode);
void ledcSetup(int channel, int freq, int resolution);
void ledcAttachPin(int pin, int channel);
void ledcWrite(int channel, int dutyCycle);
void delay(int time);
void attachCounter(int channel, int pin);
long readCounter(int channel);
void clearCounter(int channel);
int millis();
void onDispose(void (*disposeFunc)());

#define PROGMEM

class __FlashStringHelper;
#define F(string_literal) (reinterpret_cast<const __FlashStringHelper *>(string_literal))

void * 	memcpy_P (void *, const void *, size_t);

class SerialAPI {
public:
    void println();

    template<class T>
    void print(T value);
    void print(const __FlashStringHelper*);
    void print(unsigned char);
    void print(unsigned int);
};

extern SerialAPI Serial;

#define A0 13;
#define A1 12;
#define A2 14;
#define A3 27;
#define A4 26;
#define A5 25;
#define A6 33;
#define A7 32;