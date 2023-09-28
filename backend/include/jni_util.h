#pragma once

/**
 * Converts probes vector intro jni array and sends it to onIteration method
 * @returns true if iteration should continue
 */
bool send_to_plugin(JNIEnv* env, jobject obj, std::vector<Probe>* probes) {
    jclass probeClass = env->FindClass("com/paulmethfessel/elp/model/Probe");
    jfieldID codeField = env->GetFieldID(probeClass, "code", "I");
    jfieldID valueField = env->GetFieldID(probeClass, "value", "Ljava/lang/String;");
    jfieldID typeField = env->GetFieldID(probeClass, "type", "Ljava/lang/String;");

    jobjectArray jprobes = env->NewObjectArray(probes->size(), probeClass, env->AllocObject(probeClass));
    for (int i = 0; i < probes->size(); i++) {
        auto probe = (*probes)[i];
        jobject jprobe = env->AllocObject(probeClass);
        env->SetIntField(jprobe, codeField, probe.code);
        env->SetObjectField(jprobe, valueField, env->NewStringUTF(probe.value.c_str()));
        env->SetObjectField(jprobe, typeField, env->NewStringUTF(probe.type.c_str()));
        env->SetObjectArrayElement(jprobes, i, jprobe);
    }

    jclass runnerClass = env->GetObjectClass(obj);
    jmethodID methodId = env->GetMethodID(runnerClass, "onIteration", "([Lcom/paulmethfessel/elp/model/Probe;)Z");
    jboolean res = env->CallBooleanMethod(obj, methodId, jprobes);
    return res == JNI_TRUE;
}
