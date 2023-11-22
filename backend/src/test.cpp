#include <iostream>
#include "user_code.h"
#include "arduino.h"


// Run this by commenting in add_executable in CMakeLists.txt
// Will run loop once and print probes
int main() {
    // You might want to change the following path (except if your name is also paul)
    auto user_code = UserCode("/home/paul/dev/uni/embedded-live-programming-user-code/build/libcode0.so");
    user_code.setup();
    user_code.loop();
    for (const auto& probe : *user_code.probes) {
        std::cout << "Probe (" << probe.code << ", " << probe.type << "): " << probe.value << std::endl;
    }
}
