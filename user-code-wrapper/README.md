# Embedded Live Programming User Code Environment

This project is meant as the environment for the user code written by https://github.com/Paulpanther/embedded-live-programming.
The code will be copied into `src/user/`. 
`code.h` provides probes and interoperability functions, 
`arduino.h` provides Arduino functions used by the user code.
This project will be compiled every time the user code changes and loaded as a dynamic library by the runner.

## Setup
Copy https://github.com/jfjlaros/cpp-simple-rpc/tree/master/src into `src/`.
Building will be done by the plugin.
