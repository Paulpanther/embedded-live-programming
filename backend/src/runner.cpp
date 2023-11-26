#include "runner.h"
#include "user_code.h"
#include "jni_util.h"
#include <chrono>
#include <thread>
#include "arduino.h"

JNIEXPORT jint JNICALL Java_com_paulmethfessel_elp_execution_Frame_execute(JNIEnv *env, jobject obj, jstring jpath, jstring jport) {
    auto path = env->GetStringUTFChars(jpath, nullptr);
    auto port = env->GetStringUTFChars(jport, nullptr);
    // Load user code as dynamic library
    auto user_code = UserCode(path);

    // call setup (might also setup serial)
    auto interface = getInterface(port);
    if (interface == nullptr) return -1;

    user_code.setup(getInterface());
    bool keep_running;
    do {
        // loop until onIteration (in plugin) returns false
        user_code.loop();
        keep_running = send_to_plugin(env, obj, user_code.probes);
	    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    } while (keep_running);

    user_code.close();
    return 0;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    closeSerial();
}
