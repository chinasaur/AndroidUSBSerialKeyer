package net.arrl.k6pli.usbkeyeroboejava;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;
import net.arrl.k6pli.usbkeyeroboejava.Keyer;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "K6PLI Keyer";
    private ActivityMainBinding binding;
    private UsbSerialManager usbSerialManager;
    private Keyer keyer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        usbSerialManager = new UsbSerialManager(this);
        setContentView(binding.getRoot());
    }

    private void clearButtons() {
        binding.bStraightKey.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SidetoneEngine.setDefaultStreamValues(this);
        SidetoneEngine.startEngine(
                0, 0,  // Unspecified; pick the best API and OS default audio device.
                700.f);
        usbSerialManager.open();

        clearButtons();
        keyer = Keyer.straightKey(usbSerialManager);
        keyer.setupButtons(binding);
        new Thread(keyer).start();
    }

    @Override
    protected void onPause() {
        SidetoneEngine.stopEngine();
        keyer.stop();
        usbSerialManager.close();
        super.onPause();
    }
}