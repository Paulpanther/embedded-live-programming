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
    int read = interface->call<int>("analogRead", (uint8_t) pin) / 4096.0 * 255;
    return read;
    // return std::max(read + rand, 0);
}

void ledcSetup(int channel, int freq, int resolution) {
    interface->call<int>("ledcSetup", (uint8_t) channel, (uint8_t) freq, (uint8_t) resolution);
}

void ledcAttachPin(int pin, int channel) {
    interface->call<int>("ledcAttachPin", (uint8_t) pin, (uint8_t) channel);
}

void ledcWrite(int channel, int dutyCycle) {
    interface->call<int>("ledcWrite", (uint8_t) channel, (uint8_t) dutyCycle);
}

int digitalRead(int pin) {
    return interface->call<int>("digitalRead", (uint8_t) pin);
}

void digitalWrite(int pin, int value) {
    interface->call<int>("digitalWrite", (uint8_t) pin, (uint8_t) value);
}

void pinMode(int pin, int mode) {
    interface->call<int>("pinMode", (uint8_t) pin, (uint8_t) mode);
}

void attachCounter(int channel, int pin) {
    interface->call<int>("attachCounter", (uint8_t) channel, (uint8_t) pin);
}

void clearCounter(int channel) {
    interface->call<int>("clearCounter", (uint8_t) channel);
}

long readCounter(int channel) {
    return interface->call<long>("readCounter", (uint8_t) channel);
}

void delay(int time) {
    std::this_thread::sleep_for(std::chrono::milliseconds(time));
}

int time() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}
