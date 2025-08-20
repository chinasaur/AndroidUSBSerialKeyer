package net.arrl.k6pli.usbkeyeroboejava;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;

class PlaybackEngine {
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

    static native int startEngine(int audioApi, int deviceId, float frequency);
    static native int stopEngine();
    static native void playTone();
    static native void stopTone();
    static native void setDefaultStreamValues(int sampleRate, int framesPerBurst);
}


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USBKeyer";
    private ActivityMainBinding binding;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bStraightKey.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        PlaybackEngine.playTone();
                        break;
                    case (MotionEvent.ACTION_UP):
                        PlaybackEngine.stopTone();
                        break;
                }
                return true;  // Event consumed.
            }
        });
    }

    @Override
    protected void onPause() {
        PlaybackEngine.stopEngine();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlaybackEngine.setDefaultStreamValues(this);
        PlaybackEngine.startEngine(0, 0, 700.f);
    }
}