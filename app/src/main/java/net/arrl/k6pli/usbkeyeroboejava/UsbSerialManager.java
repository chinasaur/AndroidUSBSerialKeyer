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
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class UsbSerialManager {
    private static final String TAG = "UsbSerialManager";
    private static final String ACTION_USB_PERMISSION = "net.arrl.k6pli.usbkeyeroboejava.USB_PERMISSION";
    private Context context;
    private UsbDeviceConnection usbConnection = null;
    private UsbSerialPort serialPort = null;

    UsbSerialManager(Context context) {
        this.context = context;
    }

    protected void open() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
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

    protected void close() {
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
    }

    protected void serialPttDown() {
        if (serialPort == null || !serialPort.isOpen()) return;
        try {
            serialPort.setDTR(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void serialPttUp() {
        if (serialPort == null || !serialPort.isOpen()) return;
        try {
            serialPort.setDTR(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
