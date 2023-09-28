#include <jni.h>

#ifndef _Included_Frame
#define _Included_Frame
#ifdef __cplusplus
extern "C" {
#endif
    JNIEXPORT void JNICALL Java_com_paulmethfessel_elp_execution_Frame_execute(JNIEnv *, jobject, jstring);
    JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved);
#ifdef __cplusplus
}
#endif
#endif
