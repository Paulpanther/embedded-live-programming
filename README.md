# MμSE: Example-based live programming for embedded systems

This CLion plugin provides examples, live execution and probes for runtime insight.
For more information about the functionality and usage see [the paper (unpublished yet)]().

## Dev Setup
1. Clone this repo (This project only works on unix).
2. Open `rpc` in the Arduino IDE, install the `simple-rpc` library and upload the code onto a ESP32.
3. Open `backend` and run `./build.sh` to generate `runner.so`.
4. Import the gradle project in `plugin` into Intellij.
5. Run the `runIde` task with the env variables `ELP_RUNNER_PATH` and `ELP_USER_CODE_PATH` pointing to the backend and user-code-wrapper respectively (or modify and use `start.sh`).
6. When CLion starts the ESP32 has to be connected.
7. Open the Example tool-window and start coding...

## Troubleshooting
- Check if you followed the setup steps correctly
- If CLion opens, but no probe locations (yellow-dotted lines) are shown check that the CMake project is loaded
- If probe locations are shown, but no probe values the code is not executed correctly.
  - Check that the `rpc` code is running on the ESP32
  - Manually run `./build.sh code0` in `user-code-wrapper` to see if it compiles