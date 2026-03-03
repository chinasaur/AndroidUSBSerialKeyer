package net.arrl.k6pli.usbkeyeroboejava;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String VALUE_DISABLED = "disabled";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference usbDevicePref = findPreference("usb_device");
        if (usbDevicePref != null) {
            // Lazy population: refresh the list every time the user clicks to open the selection
            usbDevicePref.setOnPreferenceClickListener(preference -> {
                updateUsbDeviceList(usbDevicePref);
                return false;
            });

            usbDevicePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String deviceName = (String) newValue;
                UsbSerialManager usbManager = UsbSerialManager.getInstance(requireContext());
                
                if (VALUE_DISABLED.equals(deviceName)) {
                    usbManager.close();
                    return true;
                }
                
                List<UsbSerialDriver> drivers = usbManager.getAvailableDrivers();
                for (UsbSerialDriver driver : drivers) {
                    if (driver.getDevice().getDeviceName().equals(deviceName)) {
                        usbManager.open(driver);
                        break;
                    }
                }
                
                return true;
            });
            
            // Initial population to ensure correct summary/state when the fragment is created
            updateUsbDeviceList(usbDevicePref);
        }

        EditTextPreference frequencyPref = findPreference("sidetone_frequency");
        if (frequencyPref != null) {
            frequencyPref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            });
        }
    }

    private void updateUsbDeviceList(ListPreference usbDevicePref) {
        UsbSerialManager usbManager = UsbSerialManager.getInstance(requireContext());
        List<UsbSerialDriver> drivers = usbManager.getAvailableDrivers();
        
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();
        entries.add("Serial output disabled");
        entryValues.add(VALUE_DISABLED);
        
        for (UsbSerialDriver driver : drivers) {
            UsbDevice device = driver.getDevice();
            String manufacturer = device.getManufacturerName();
            String product = device.getProductName();
            
            String label;
            if (manufacturer != null && !manufacturer.isEmpty() && product != null && !product.isEmpty()) {
                label = manufacturer + " " + product;
            } else if (product != null && !product.isEmpty()) {
                label = product;
            } else {
                label = "Serial Device (VID:" + String.format("%04X", device.getVendorId()) +
                        " PID:" + String.format("%04X", device.getProductId()) + ")";
            }
            entries.add(label);
            entryValues.add(device.getDeviceName());
        }
        usbDevicePref.setEntries(entries.toArray(new CharSequence[0]));
        usbDevicePref.setEntryValues(entryValues.toArray(new CharSequence[0]));
    }
}
