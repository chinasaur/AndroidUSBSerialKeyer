/*
 * Copyright (C) 2025 Peter K6PLI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_playSidetoneNative(
        JNIEnv *env,
        jclass) {
    sidetoneEngine.playSidetone();
}

JNIEXPORT void JNICALL
Java_net_arrl_k6pli_usbkeyeroboejava_SidetoneEngine_playSilenceNative(
        JNIEnv *env,
        jclass) {
    sidetoneEngine.playSilence();
}

}
