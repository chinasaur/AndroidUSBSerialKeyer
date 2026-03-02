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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class UsbSerialManager {
    private static final String TAG = "UsbSerialManager";
    public static final String ACTION_USB_PERMISSION = "net.arrl.k6pli.usbkeyeroboejava.USB_PERMISSION";
    public static final String VALUE_DISABLED = "disabled";

    private static UsbSerialManager instance;
    private final Context context;
    private UsbDeviceConnection usbConnection = null;
    private UsbSerialPort serialPort = null;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d(TAG, "Permission granted for device " + device.getDeviceName());
                            // Try to open the device now that we have permission
                            openByDevice(device);
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device);
                    }
                }
            }
        }
    };

    public static synchronized UsbSerialManager getInstance(Context context) {
        if (instance == null) {
            instance = new UsbSerialManager(context);
        }
        return instance;
    }

    private UsbSerialManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void registerReceiver() {
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(usbPermissionReceiver, filter);
            }
            receiverRegistered = true;
        }
    }

    public void unregisterReceiver() {
        if (receiverRegistered) {
            context.unregisterReceiver(usbPermissionReceiver);
            receiverRegistered = false;
        }
    }

    public List<UsbSerialDriver> getAvailableDrivers() {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return UsbSerialProber.getDefaultProber().findAllDrivers(manager);
    }

    protected void open() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String preferredDeviceName = prefs.getString("usb_device", null);
        if (VALUE_DISABLED.equals(preferredDeviceName)) return;

        List<UsbSerialDriver> drivers = getAvailableDrivers();
        if (drivers.isEmpty()) return;

        UsbSerialDriver driverToOpen = null;
        if (preferredDeviceName != null) {
            for (UsbSerialDriver driver : drivers) {
                if (driver.getDevice().getDeviceName().equals(preferredDeviceName)) {
                    driverToOpen = driver;
                    break;
                }
            }
        }

        // Fallback to first available if preferred not found or not set
        if (driverToOpen == null) {
            driverToOpen = drivers.get(0);
        }

        open(driverToOpen);
    }

    public void openByDevice(UsbDevice device) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        for (UsbSerialDriver driver : drivers) {
            if (driver.getDevice().equals(device)) {
                open(driver);
                return;
            }
        }
    }

    public void open(UsbSerialDriver driver) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice device = driver.getDevice();

        if (!manager.hasPermission(device)) {
            Log.d(TAG, "Requesting permission for device " + device.getDeviceName());
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            manager.requestPermission(device, permissionIntent);
            return;
        }

        // Close existing if any
        close();

        usbConnection = manager.openDevice(device);
        if (usbConnection == null) {
            Log.w(TAG, "Failed to open USB connection");
            return;
        }

        serialPort = driver.getPorts().get(0);
        try {
            serialPort.open(usbConnection);
            serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            serialPort.setDTR(false);
            serialPort.setRTS(false);
        } catch (IOException e) {
            Log.w(TAG, "Open serial port failed: " + e.getMessage());
            close();
            return;
        }
        Log.i(TAG, "Serial port ready.");
    }

    public void close() {
        if (serialPort != null) {
            try {
                if (serialPort.isOpen()) serialPort.close();
            } catch (IOException ignored) {}
            serialPort = null;
        }
        if (usbConnection != null) {
            usbConnection.close();
            usbConnection = null;
        }
    }

    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen();
    }

    protected void serialPttDown() {
        if (serialPort == null || !serialPort.isOpen()) return;
        try {
            serialPort.setDTR(true);
        } catch (IOException e) {
            Log.e(TAG, "serialPttDown failed", e);
            close();
        }
    }

    protected void serialPttUp() {
        if (serialPort == null || !serialPort.isOpen()) return;
        try {
            serialPort.setDTR(false);
        } catch (IOException e) {
            Log.e(TAG, "serialPttUp failed", e);
            close();
        }
    }
}
