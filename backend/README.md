This runner is supposed to run parallel to the Clion-plugin.
It gets loaded by it as a dynamic library and executed through JNI.
`runner.cpp` is the entrypoint. It loads the user code as a dynamic lib given a path and executes it.
The runner also manages the serial connection.

## Setup
Only works on unix
- Run `build.sh`
