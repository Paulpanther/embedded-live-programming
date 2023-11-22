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

int analogRead(int pin);
int digitalRead(int pin);
void digitalWrite(int pin, int value);
void pinMode(int pin, int mode);
void ledcSetup(int channel, int freq, int resolution);
void ledcAttachPin(int pin, int channel);
void ledcWrite(int channel, int dutyCycle);
void attachCounter(int channel, int pin);
long readCounter(int channel);
void clearCounter(int channel);
void delay(int time);
int time();

void initSerial();
void closeSerial();
