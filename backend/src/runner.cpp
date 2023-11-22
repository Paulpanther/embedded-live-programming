#include "runner.h"
#include "user_code.h"
#include "jni_util.h"
#include <chrono>
#include <thread>
#include <iostream>

JNIEXPORT void JNICALL Java_com_paulmethfessel_elp_execution_Frame_execute(JNIEnv *env, jobject obj, jstring jpath) {
    std::cout << "JNI START" << std::endl;

    auto path = env->GetStringUTFChars(jpath, nullptr);
    // Load user code as dynamic library
    auto user_code = UserCode(path);

    // call setup (might also setup serial)
    std::cout << "JNI SETUP" << std::endl;
    user_code.setup();
    bool keep_running;
    std::cout << "JNI LOOP" << std::endl;
    do {
        // loop until onIteration (in plugin) returns false
        user_code.loop();
        keep_running = send_to_plugin(env, obj, user_code.probes);
	std::this_thread::sleep_for(std::chrono::milliseconds(10));
    } while (keep_running);

    std::cout << "JNI CLOSE" << std::endl;
    user_code.close();
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
}
