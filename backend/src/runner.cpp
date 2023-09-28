#include "runner.h"
#include "user_code.h"
#include "jni_util.h"
#include <chrono>
#include <thread>
#include "arduino.h"

JNIEXPORT void JNICALL Java_com_paulmethfessel_elp_execution_Frame_execute(JNIEnv *env, jobject obj, jstring jpath) {
    auto path = env->GetStringUTFChars(jpath, nullptr);
    // Load user code as dynamic library
    auto user_code = UserCode(path);

    // call setup (might also setup serial)
    user_code.setup(getInterface());
    bool keep_running;
    do {
        // loop until onIteration (in plugin) returns false
        user_code.loop();
        keep_running = send_to_plugin(env, obj, user_code.probes);
	    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    } while (keep_running);

    user_code.close();
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    closeSerial();
}
