# TL;DR
Provides a simple Android app for Morse code / CW keying.
My main goal was to provide iambic keying on the Quansheng UV-K5 with [IJV 2.9R5](https://www.universirius.com/en_gb/preppers/quansheng-uv-k5-manuale-del-firmware-ijv_2-9r5/) firmware for 2m/70cm CW.
This app connects to the Quansheng via an [All-In-One-Cable (AIOC)](https://github.com/skuep/AIOC) as the hardware intermediary.
It may work to use this as a backup key with other rigs, with an appropriate custom cable.
It can also be used as a practice sidetone oscillator.
Developed with Android Studio in Java and C++.

# Some details
Current keying options are straight key or iambic (A) mode.
(Cootie and bug are implemented but not enabled as the UI needs work.)
The keying is implemented as a background Java thread state machine.
It seems to work reasonably at >25 WPM running on a Pixel 9 phone.

The app provides local sidetone with raised cosine envelope via the Oboe native low-latency audio interface.
However, for now when connected to the Quansheng it is recommended to clip the 3.5 TRS connector to just the sleeve so that you can hear the incoming audio;
In this case you can also just use the Quansheng's provided sidetone.
Alternative audio configurations are under consideration (e.g. routing the Quansheng incoming audio to the phone speakers).

The app connects to the AIOC hardware via [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android).
The AIOC is designed to PTT when the DTR line is pulled; see [AIOC documentation](https://github.com/skuep/AIOC) for details.
So we just twiddle DTR to CW key the Quansheng IJV and latency seems fine.
