###Microsoft band sensor->OSC sender.

Based on BandAccelerometerApp from SDK examples. I simply took the example app and integrated the OSC sending functionality. Currently streams the following sensors:

- Accelerometer
- Gyro
- Heart Rate
- Motion (type and speed)
- Skin Temperature
- Ambient light
- GSR

Not implemented:

- RR
- Heart Rate Quality
- Altimeter

Nice to have features/TODO:

- Interface for changing gyro/accel sample rate
- Investigate the usage of AsyncTask for transmitting messages, my gut feeling says a persistent threaded sender object might be faster
- UI for turning on/off various sensors

Using javaosc (http://www.illposed.com/software/javaosc.html)

Contains a Max/MSP patch for testing. Make sure the android device and host computer is on the same local network and IP/port settings are correct.


Mar 2015
Johnty Wang,
Input Devices and Music Interaction Laboratory,
McGill University

johnty.wang@mail.mcgill.ca
