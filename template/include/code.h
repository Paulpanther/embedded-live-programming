#pragma once

#include <chrono>
#include <cmath>

#define HIGH 1
#define LOW 0
#define INPUT 1
#define OUTPUT 2
#define INPUT_PULLUP 5

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
int time();
void onDispose(void (*disposeFunc)());