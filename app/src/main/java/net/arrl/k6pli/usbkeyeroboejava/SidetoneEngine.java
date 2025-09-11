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
package net.arrl.k6pli.usbkeyeroboejava;

import android.content.Context;
import android.media.AudioManager;

public class SidetoneEngine {
    static {
        System.loadLibrary("usbkeyeroboejava");
    }

    static void setDefaultStreamValues(Context context) {
        AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int defaultSampleRate = Integer.parseInt(sampleRateStr);
        String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);
        setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst);
    }

    static native void setDefaultStreamValues(int sampleRate, int framesPerBurst);
    static native int startEngine(int audioApi, int deviceId, float frequency);
    static native int stopEngine();
    static native void playSidetone();
    static native void playSilence();
}
