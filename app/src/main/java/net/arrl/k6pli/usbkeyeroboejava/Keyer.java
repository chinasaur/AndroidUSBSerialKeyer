package net.arrl.k6pli.usbkeyeroboejava;


import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;


abstract public class Keyer implements Runnable {
    public static Keyer straightKey(UsbSerialManager usbSerialManager) {
        return new StraightKey(usbSerialManager);
    }

    private final UsbSerialManager usbSerialManager;

    public Keyer(UsbSerialManager usbSerialManager) {
        this.usbSerialManager = usbSerialManager;
    }

    public void run() {}
    public void stop() {}
    public abstract void setupButtons(ActivityMainBinding binding);

    public void keyDown() {
        SidetoneEngine.playSidetone();
        this.usbSerialManager.serialPttDown();
    }

    public void keyUp() {
        SidetoneEngine.playSilence();
        this.usbSerialManager.serialPttUp();
    }
}


class StraightKey extends Keyer {
    public StraightKey(UsbSerialManager usbSerialManager) { super(usbSerialManager); }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setupButtons(ActivityMainBinding binding) {
        binding.bStraightKey.setVisibility(View.VISIBLE);
        binding.bStraightKey.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    keyDown();
                    break;
                case (MotionEvent.ACTION_UP):
                    keyUp();
                    break;
            }
            return true;  // Event consumed.
        });
    }
}