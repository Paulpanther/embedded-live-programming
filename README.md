# Embedded Live Programming
Implementation of my Master Thesis about a live programming environment for embedded programming.
Enables user to write examples and replacements for classes to run them in isolation from other components.
Code is always executed live (on save) and interesting parts of code are annotated with probes that show the current value of that expression.

Code is written in C++ and uses Arduino-style API functions (`analogRead` and co). Code is executed live on the developer machine, only Arduino-API calls are send to the ESP via rpc.

## Setup
- Clone this repo, the [runner](https://github.com/Paulpanther/embedded-live-programming-runner), [user code](https://github.com/Paulpanther/embedded-live-programming-user-code), [Arduino code](https://github.com/Paulpanther/embedded-live-programming-mcu) and [demo template](https://github.com/Paulpanther/embedded-live-programming-template).
- Setup each repo
- Execute `runPlugin` task

<!-- Plugin description -->
<!-- Plugin description end -->

## Execution Pipeline
1. User saves files, `FileChangeListener` is run, starts `CodeExecutionManager`
2. hash is checked to not re-run same files
3. Files are copied to not modify original files
4. Probe locations are found, instrumented and registered
5. Replacements are applied and modifications stored
6. Runner file that executes current example is created
7. Generated code files are placed in user code environment
8. User code get build to a dyn-lib
9. C++ Runner is called with path to dyn-lib, to execute it
10. C++ Runner will initiate serial connection to ESP on first run
11. C++ Runner runs setup, then loop. After each loop probes are reported to plugin
12. Once the plugin receives probes, the values are stored in the registered probes and presentations are redrawn Sparklines if present
13. After the execution the CodeExecutionManager also requests a redraw of presentations
14. The `ProbeInlayProvider` will attach Sparklines to the registered probes


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
