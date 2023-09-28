#include <simpleRPC.h>


long counter0 = 0;
long counter1 = 0;

void IRAM_ATTR interruptChannel0() {
  counter0++;
}

void IRAM_ATTR interruptChannel1() {
  counter1++;
}

int interrupt0Pin = -1;
int interrupt1Pin = -1;
int attachCounter(int channel, int pin) {
  if (channel == 0) {
    if (interrupt0Pin != -1) detachInterrupt(interrupt0Pin);
    interrupt0Pin = pin;
    attachInterrupt(pin, interruptChannel0, RISING);
  } else if (channel == 1) {
    if (interrupt1Pin != -1) detachInterrupt(interrupt1Pin);
    interrupt1Pin = pin;
    attachInterrupt(pin, interruptChannel1, RISING);
  }
  return 1;
}

long readCounter(int channel) {
  if (channel == 0) {
    return counter0;
  } else {
    return counter1;
  }
}

int clearCounter(int channel) {
  if (channel == 0) {
    counter0 = 0;
  } else {
    counter1 = 0;
  }
  return 1;
}

void setup() {
  Serial.begin(9600);
}

int _digitalWrite(int pin, int value) {
  digitalWrite(pin, value);
  return 1;
}

int _ledcSetup(int channel, int freq, int resolution_bits) {
  ledcSetup(channel, freq, resolution_bits);
  return 1;
}

int _ledcAttachPin(int pin, int channel) {
  ledcAttachPin(pin, channel);
  return 1;
}

int _ledcWrite(int channel, int dutyCycle) {
  ledcWrite(channel, dutyCycle);
  return 1;
}

int _pinMode(int pin, int mode) {
  pinMode(pin, mode);
  return 1;
}

void loop() {
  // Because of flush-delays in the protocol every function should return a value to reduce latency
  interface(Serial, 
    digitalRead, "digitalRead: Read digital pin. @pin: Pin number. @return: Pin value.", 
    analogRead, "analogRead: Read analog pin. @pin: Pin number. @return: Pin value.", 
    _digitalWrite, "digitalWrite: Write to digital pin. @pin: Pin number. @value: Value to write. @return: Success.",
    _ledcSetup, "ledcSetup: Create PWM channel. @channel: Channel. @freq: Frequency. @resolution_bits: Resolution. @return: Success.",
    _ledcAttachPin, "ledcAttachPin: Attach pin to channel. @pin: Pin. @channel: Channel. @return: Success.",
    _ledcWrite, "ledcWrite: Write to PWM pin. @channel: Channel. @dutyCycle: Duty Cycle. @return: Success.",
    _pinMode, "pinMode: Set pin mode. @pin: Pin number. @mode: Mode to set. @return: Success.",
    attachCounter, "attachCounter: attachCounter. @channel: channel. @pin: pin. @return: Success.",
    readCounter, "readCounter: readCounter. @channel: channel. @return: value.",
    clearCounter, "clearCounter: clearCounter. @channel: channel. @return: Success.");
}