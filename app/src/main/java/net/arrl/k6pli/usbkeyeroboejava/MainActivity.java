package net.arrl.k6pli.usbkeyeroboejava;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;

import java.util.List;

class SidetoneEngine {
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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USBKeyer";
    private static final String ACTION_USB_PERMISSION = "net.arrl.k6pli.usbkeyeroboejava.USB_PERMISSION";
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
                        SidetoneEngine.playSidetone();
                        break;
                    case (MotionEvent.ACTION_UP):
                        SidetoneEngine.playSilence();
                        break;
                }
                return true;  // Event consumed.
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SidetoneEngine.setDefaultStreamValues(this);
        SidetoneEngine.startEngine(
                0,  // Unspecified; pick the best available API
                0,  // Unspecified; pick the first available device
                700.f);

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.i(TAG, "No USB devices found");
            return;
        }
    }

    @Override
    protected void onPause() {
        SidetoneEngine.stopEngine();
        super.onPause();
    }
}