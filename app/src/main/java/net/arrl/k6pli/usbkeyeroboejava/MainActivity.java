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

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "K6PLI Keyer";
    private ActivityMainBinding binding;
    private UsbSerialManager usbSerialManager;
    private Keyer keyer;

    private void setupKeyerTypeUI() {
        binding.rgKeyerType.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding.rgKeyerType.getCheckedRadioButtonId() == R.id.rbIambicA) {
                binding.llWPM.setVisibility(View.VISIBLE);
            } else {
                binding.llWPM.setVisibility(View.INVISIBLE);
            }
            setupKeyer();
        });
    }

    private void setupWPM() {
        binding.sbWPM.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvWPM.setText(i + " WPM");
                setupKeyer();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupKeyerTypeUI();
        setupWPM();
        usbSerialManager = new UsbSerialManager(this);
    }

    private void resetButtons() {
        binding.bStraightKey.setVisibility(View.INVISIBLE);
        binding.bLeftPaddle.setVisibility(View.INVISIBLE);
        binding.bRightPaddle.setVisibility(View.INVISIBLE);
        binding.bLeftPaddle.setText("");
        binding.bRightPaddle.setText("");
    }

    private void setupKeyer() {
        resetButtons();
        if (keyer != null) keyer.stop();
        keyer = Keyer.buildKeyer(binding, usbSerialManager, binding.sbWPM.getProgress());
        keyer.setupButtons(binding);
        new Thread(keyer).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SidetoneEngine.setDefaultStreamValues(this);
        SidetoneEngine.startEngine(
                0, 0,  // Unspecified; pick the best API and OS default audio device.
                700.f);
        usbSerialManager.open();
        setupKeyer();
    }

    @Override
    protected void onPause() {
        SidetoneEngine.stopEngine();
        keyer.stop();
        usbSerialManager.close();
        super.onPause();
    }
}
