#include <jni.h>
#include <oboe/Oboe.h>
#include <string>
#include "SidetoneEngine.h"

static SidetoneEngine sidetoneEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_setDefaultStreamValues(
        JNIEnv *env,
        jclass,
        jint sampleRate,
        jint framesPerBurst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
}

JNIEXPORT jint JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_startEngine(
        JNIEnv *env,
        jclass,
        int audioApi, int deviceId, float frequency) {
    return static_cast<jint>(sidetoneEngine.start((oboe::AudioApi) audioApi, deviceId, frequency));
}

JNIEXPORT jint JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_stopEngine(
        JNIEnv *env,
        jclass) {
    return static_cast<jint>(sidetoneEngine.stop());
}

JNIEXPORT void JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_playSidetone(
        JNIEnv *env,
        jclass,
        jboolean isToneOn) {
    sidetoneEngine.playSidetone();
}
JNIEXPORT void JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_playSilence(
        JNIEnv *env,
        jclass,
        jboolean isToneOn) {
    sidetoneEngine.playSilence();
}

}
