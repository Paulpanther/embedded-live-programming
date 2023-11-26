#include "arduino.h"
#include <chrono>
#include <thread>
#include <random>

Interface* interface;

std::random_device rd; // obtain a random number from hardware
std::mt19937 gen(rd()); // seed the generator
std::uniform_int_distribution<> distr(-11, 5);

void setInterface(Interface* i) {
    interface = i;
}

int analogRead(int pin) {
    // int rand = distr(gen);
    int read = interface->call<int32_t>("analogRead", (int32_t) pin) / 4096.0 * 255;
    return read;
    // return std::max(read + rand, 0);
}

void ledcSetup(int channel, int freq, int resolution) {
    interface->call<int32_t>("ledcSetup", (int32_t) channel, (int32_t) freq, (int32_t) resolution);
}

void ledcAttachPin(int pin, int channel) {
    interface->call<int32_t>("ledcAttachPin", (int32_t) pin, (int32_t) channel);
}

void ledcWrite(int channel, int dutyCycle) {
    interface->call<int32_t>("ledcWrite", (int32_t) channel, (int32_t) dutyCycle);
}

int digitalRead(int pin) {
    return interface->call<int32_t>("digitalRead", (int32_t) pin);
}

void digitalWrite(int pin, int value) {
    interface->call<int32_t>("digitalWrite", (int32_t) pin, (int32_t) value);
}

void pinMode(int pin, int mode) {
    interface->call<int32_t>("pinMode", (int32_t) pin, (int32_t) mode);
}

void attachCounter(int channel, int pin) {
    interface->call<int32_t>("attachCounter", (int32_t) channel, (int32_t) pin);
}

void clearCounter(int channel) {
    interface->call<int32_t>("clearCounter", (int32_t) channel);
}

long readCounter(int channel) {
    return interface->call<long>("readCounter", (int32_t) channel);
}

void delay(int time) {
    std::this_thread::sleep_for(std::chrono::milliseconds(time));
}

int time() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}
