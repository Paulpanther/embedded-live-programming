#include <jni.h>

#ifndef _Included_Frame
#define _Included_Frame
#ifdef __cplusplus
extern "C" {
#endif
    JNIEXPORT jint JNICALL Java_com_paulmethfessel_elp_execution_Frame_execute(JNIEnv *, jobject, jstring, jstring);
    JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved);
#ifdef __cplusplus
}
#endif
#endif
