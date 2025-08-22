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

import java.io.IOException;
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
    private UsbDeviceConnection usbConnection = null;
    private UsbSerialPort serialPort = null;

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
                        if (serialPort != null && serialPort.isOpen()) {
                            try {
                                serialPort.setDTR(true);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case (MotionEvent.ACTION_UP):
                        SidetoneEngine.playSilence();
                        if (serialPort != null && serialPort.isOpen()) {
                            try {
                                serialPort.setDTR(false);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
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
                0, 0,  // Unspecified; pick the best API and OS default audio device.
                700.f);

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        manager.requestPermission(driver.getDevice(), permissionIntent);
        usbConnection = manager.openDevice(driver.getDevice());
        if (usbConnection == null) {
            Log.w(TAG, "USB connection permission not granted");
            return;
        }
        // Open serial port.
        serialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            serialPort.open(usbConnection);
            serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            serialPort.setDTR(false);
            serialPort.setRTS(false);
        } catch (IOException e) {
            Log.w(TAG, "Open serial port failed.");
            return;
        }
        Log.i(TAG, "Serial port ready.");
    }

    @Override
    protected void onPause() {
        SidetoneEngine.stopEngine();
        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            serialPort = null;
        }
        if (usbConnection != null) {
            usbConnection.close();
            usbConnection = null;
        }
        super.onPause();
    }
}