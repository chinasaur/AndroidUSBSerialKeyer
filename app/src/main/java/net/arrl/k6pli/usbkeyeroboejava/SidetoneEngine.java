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
