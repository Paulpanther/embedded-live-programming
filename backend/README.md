# Runner for ELP
This runner is supposed to run parallel to the Clion-plugin at https://github.com/Paulpanther/embedded-live-programming. 
It gets loaded by it as a dynamic library and executed through JNI.
`runner.cpp` is the entrypoint. It loads the user code as a dynamic lib given a path and executes it.
The runner also manages the serial connection.

## Setup
Only works on unix
- Copy files from https://github.com/jfjlaros/cpp-simple-rpc/tree/master/src into `src/`
- Run `build.sh`
