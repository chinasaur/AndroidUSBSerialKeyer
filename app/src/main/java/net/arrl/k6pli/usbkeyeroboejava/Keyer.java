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


import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

import net.arrl.k6pli.usbkeyeroboejava.databinding.ActivityMainBinding;


abstract public class Keyer implements Runnable {
    public static Keyer buildKeyer(
            String keyerType, UsbSerialManager usbSerialManager, int wordsPerMinute) {
        if ("iambic_a".equals(keyerType)) {
            return new IambicA(usbSerialManager, wordsPerMinute);
        } else if ("straight".equals(keyerType)) {
            return new StraightKey(usbSerialManager);
        } else {
            return new StraightKey(usbSerialManager);
        }
    }

    private final UsbSerialManager usbSerialManager;

    public Keyer(UsbSerialManager usbSerialManager) {
        this.usbSerialManager = usbSerialManager;
    }

    public void run() {}
    public void stop() {}

    public void resetButtons(ActivityMainBinding binding) {
        binding.bStraightKey.setVisibility(View.INVISIBLE);
        binding.bLeftPaddle.setVisibility(View.INVISIBLE);
        binding.bRightPaddle.setVisibility(View.INVISIBLE);
        binding.bLeftPaddle.setText("");
        binding.bRightPaddle.setText("");
    }

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
        resetButtons(binding);
        binding.bStraightKey.setVisibility(View.VISIBLE);
        binding.bLeftPaddle.setVisibility(View.INVISIBLE);
        binding.bRightPaddle.setVisibility(View.INVISIBLE);
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


class Cootie extends Keyer {
    public Cootie(UsbSerialManager usbSerialManager) {
        super(usbSerialManager);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setupButtons(ActivityMainBinding binding) {
        resetButtons(binding);
        binding.bLeftPaddle.setVisibility(View.VISIBLE);
        binding.bRightPaddle.setVisibility(View.VISIBLE);
        View.OnTouchListener listener = (v, event) -> {
            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    keyDown();
                    break;
                case (MotionEvent.ACTION_UP):
                    keyUp();
                    break;
            }
            return true;  // Event consumed.
        };
        binding.bLeftPaddle.setOnTouchListener(listener);
        binding.bRightPaddle.setOnTouchListener(listener);
    }
}


enum KeyState {
    IDLE, DIT, DAH, SPACE_AFTER_DIT, SPACE_AFTER_DAH
}


abstract class PaddleKeyer extends Keyer {
    protected final long ditLengthNanos;
    protected volatile boolean keepRunning = true;
    protected volatile boolean ditOn = false;
    protected volatile boolean dahOn = false;
    protected long stateHoldUntilNanoTime = 0;
    protected KeyState keyState = KeyState.IDLE;

    public PaddleKeyer(UsbSerialManager usbSerialManager, int wordsPerMinute) {
        super(usbSerialManager);
        this.ditLengthNanos = (long) (1200.f / wordsPerMinute * 1e6);
    }

    @Override
    public void stop() {
        keepRunning = false;
    }

    protected void ditOn() {
        ditOn = true;
    }

    protected void ditOff() {
        ditOn = false;
    }

    protected void dahOn() {
        dahOn = true;
    }

    protected void dahOff() {
        dahOn = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setupButtons(ActivityMainBinding binding) {
        resetButtons(binding);
        binding.bLeftPaddle.setVisibility(View.VISIBLE);
        binding.bRightPaddle.setVisibility(View.VISIBLE);
        binding.bLeftPaddle.setText(".");
        binding.bRightPaddle.setText("-");
        binding.bLeftPaddle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    ditOn();
                    break;
                case (MotionEvent.ACTION_UP):
                    ditOff();
                    break;
            }
            return true;  // Event consumed.
        });
        binding.bRightPaddle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    dahOn();
                    break;
                case (MotionEvent.ACTION_UP):
                    dahOff();
                    break;
            }
            return true;  // Event consumed.
        });
    }
}


class Bug extends PaddleKeyer {
    public Bug(UsbSerialManager usbSerialManager, int wordsPerMinute) {
        super(usbSerialManager, wordsPerMinute);
    }

    @Override
    public void run() {
        while (keepRunning) {
            switch (keyState) {
                case IDLE:
                    if (ditOn) {
                        keyDown();
                        keyState = KeyState.DIT;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    }
                    if (dahOn) {
                        keyDown();
                        keyState = KeyState.DAH;
                    }
                    break;
                case DIT:
                    if (dahOn || System.nanoTime() > stateHoldUntilNanoTime) {
                        keyUp();
                        keyState = KeyState.SPACE_AFTER_DIT;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    }
                    break;
                case DAH:
                    if (ditOn) {
                        keyUp();
                        dahOff();
                        keyState = KeyState.SPACE_AFTER_DAH;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    } else if (!dahOn) {
                        keyUp();
                        keyState = KeyState.IDLE;
                    }
                    break;
                case SPACE_AFTER_DIT:
                case SPACE_AFTER_DAH:
                    if (System.nanoTime() > stateHoldUntilNanoTime) {
                        keyState = KeyState.IDLE;
                    }
                    break;
            }

            try {
                Thread.sleep(0, 10 * 1000);  // Sleep 10 us.
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


class IambicA extends PaddleKeyer {
    private boolean ditMemory = false;
    private boolean dahMemory = false;

    public IambicA(UsbSerialManager usbSerialManager, int wordsPerMinute) {
        super(usbSerialManager, wordsPerMinute);
    }

    @Override
    public void run() {
        while (keepRunning) {
            switch (keyState) {
                case IDLE:
                    if (ditOn) {
                        keyDown();
                        keyState = KeyState.DIT;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    }
                    if (dahOn) {
                        keyDown();
                        keyState = KeyState.DAH;
                        stateHoldUntilNanoTime = System.nanoTime() + 3 * ditLengthNanos;
                    }
                    break;
                case DIT:
                    if (dahOn) dahMemory = true;
                    if (System.nanoTime() > stateHoldUntilNanoTime) {
                        keyUp();
                        keyState = KeyState.SPACE_AFTER_DIT;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    }
                    break;
                case DAH:
                    if (ditOn) ditMemory = true;
                    if (System.nanoTime() > stateHoldUntilNanoTime) {
                        keyUp();
                        keyState = KeyState.SPACE_AFTER_DAH;
                        stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                    }
                    break;
                case SPACE_AFTER_DIT:
                    if (dahOn) dahMemory = true;
                    if (System.nanoTime() > stateHoldUntilNanoTime) {
                        if (dahMemory) {
                            keyDown();
                            dahMemory = false;
                            keyState = KeyState.DAH;
                            stateHoldUntilNanoTime = System.nanoTime() + 3 * ditLengthNanos;
                        } else {
                            keyState = KeyState.IDLE;
                        }
                    }
                    break;
                case SPACE_AFTER_DAH:
                    if (ditOn) ditMemory = true;
                    if (System.nanoTime() > stateHoldUntilNanoTime) {
                        if (ditMemory) {
                            keyDown();
                            ditMemory = false;
                            keyState = KeyState.DIT;
                            stateHoldUntilNanoTime = System.nanoTime() + ditLengthNanos;
                        } else {
                            keyState = KeyState.IDLE;
                        }
                    }
                    break;
            }

            try {
                Thread.sleep(0, 10 * 1000);  // Sleep 10 us.
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
