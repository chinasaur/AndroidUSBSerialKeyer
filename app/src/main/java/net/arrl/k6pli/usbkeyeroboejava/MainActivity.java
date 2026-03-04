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
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences sharedPrefs;
    private ActivityMainBinding binding;
    private UsbSerialManager usbSerialManager;
    private Keyer keyer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        usbSerialManager = UsbSerialManager.getInstance(this);
        setupKeyerControls();
        
        binding.ibSettings.setOnClickListener(v -> 
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void setupKeyerControls() {
        String[] entries = getResources().getStringArray(R.array.keyer_modes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, entries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spKeyerType.setAdapter(adapter);

        binding.spKeyerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] entryValues = getResources().getStringArray(R.array.keyer_modes_values);
                String valueToSave = entryValues[position];
                String currentPref = sharedPrefs.getString("keyer_mode", "straight");
                if (!valueToSave.equals(currentPref)) {
                    sharedPrefs.edit().putString("keyer_mode", valueToSave).apply();
                    updateWpmVisibility(valueToSave);
                    setupKeyer();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.sbWPM.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Enforce minimum WPM for backward compatibility (API < 26)
                if (progress < 5) {
                    progress = 5;
                    seekBar.setProgress(5);
                }

                if (fromUser) {
                    sharedPrefs.edit().putInt("keyer_wpm", progress).apply();
                }
                binding.tvWPM.setText(progress + " WPM");
                setupKeyer();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        refreshUiFromPrefs();
    }

    private void refreshUiFromPrefs() {
        String savedValue = sharedPrefs.getString("keyer_mode", "straight");
        String[] entryValues = getResources().getStringArray(R.array.keyer_modes_values);
        int index = -1;
        for (int i = 0; i < entryValues.length; ++i) {
            if (entryValues[i].equals(savedValue)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            binding.spKeyerType.setSelection(index);
        }
        updateWpmVisibility(savedValue);
        
        int savedSpeed = sharedPrefs.getInt("keyer_wpm", 20);
        binding.sbWPM.setProgress(savedSpeed);
        binding.tvWPM.setText(savedSpeed + " WPM");
    }

    private void updateWpmVisibility(String keyer_mode) {
        int visibility = keyer_mode.equals("straight") ? View.INVISIBLE : View.VISIBLE;
        binding.llWPM.setVisibility(visibility);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        SidetoneEngine.setDefaultStreamValues(this);

        int frequency = getSidetoneFrequency();
        SidetoneEngine.startEngine(0, 0, frequency);

        usbSerialManager.registerReceiver();
        usbSerialManager.open();

        refreshUiFromPrefs();
        setupKeyer();
        updateSidetoneState();
    }

    @Override
    protected void onPause() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        SidetoneEngine.stopEngine();
        if (keyer != null) keyer.stop();
        usbSerialManager.unregisterReceiver();
        usbSerialManager.close();
        super.onPause();
    }

    private void setupKeyer() {
        if (keyer != null) keyer.stop();
        String[] entryValues = getResources().getStringArray(R.array.keyer_modes_values);
        int selectedIndex = binding.spKeyerType.getSelectedItemPosition();
        String selectedValue = (selectedIndex >= 0 && selectedIndex < entryValues.length)
                ? entryValues[selectedIndex] : "straight";
        keyer = Keyer.buildKeyer(selectedValue, usbSerialManager, binding.sbWPM.getProgress());
        keyer.setupButtons(binding);
        new Thread(keyer).start();
    }

    private void updateSidetoneState() {
        String mode = sharedPrefs.getString("sidetone_mode", "auto");
        boolean enabled;
        switch (mode) {
            case "on":
                enabled = true;
                break;
            case "off":
                enabled = false;
                break;
            case "auto":
            default:
                enabled = !usbSerialManager.isConnected();
                break;
        }
        SidetoneEngine.setSidetoneEnabled(enabled);
    }

    private int getSidetoneFrequency() {
        try {
            String freqStr = sharedPrefs.getString("sidetone_frequency", "700");
            return Integer.parseInt(freqStr);
        } catch (NumberFormatException e) {
            return 700;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("sidetone_mode".equals(key)) {
            updateSidetoneState();
        } else if ("usb_device".equals(key)) {
            usbSerialManager.open();
            updateSidetoneState();
        } else if ("sidetone_frequency".equals(key)) {
            SidetoneEngine.setFrequency(getSidetoneFrequency());
        }
    }
}
