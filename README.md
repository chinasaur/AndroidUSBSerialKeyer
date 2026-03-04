# TL;DR
Provides a simple Android app for Morse code / CW keying.
My original goal was to provide iambic keying for the Quansheng UV-K5 with 
[IJV 2.9R5](https://www.universirius.com/en_gb/preppers/quansheng-uv-k5-manuale-del-firmware-ijv_2-9r5/) firmware via an 
[All-In-One-Cable (AIOC)](https://github.com/skuep/AIOC) as the hardware intermediary.
This can also be used as a backup CW key for rigs with a direct USB keying option, such as the [QMX](https://qrp-labs.com/qmx)/QMX+ with firmware 1_03 or later.
It can also be used as a practice sidetone oscillator.
Developed with Android Studio in Java and C++.

# Some details
Current keying options are straight key or iambic (A) mode.
(Cootie and bug are implemented but not enabled as the UI needs work.)
The keying is implemented as a background Java thread state machine.
It works smoothly at >25 WPM running on a Pixel 9 phone.

The app provides local sidetone with raised cosine envelope via the Oboe native low-latency audio interface.
When connected to a rig via USB device manager, local sidetone by default is disabled, but this can be controlled by setting.
(Note that if the rig also provides a USB audio interface, you may need to manually set the audio output device back to the phone speaker.)

The app connects to USB serial hardware via [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android).
The AIOC is designed to PTT when the DTR line is pulled; see [AIOC documentation](https://github.com/skuep/AIOC) for details.
Many rigs also support DTR keying.
QMX/QMX+ with firmware 1_03 can have DTR keying enabled in settings and then it works the same as AIOC with this app.
Additional USB based keying support could be added.
