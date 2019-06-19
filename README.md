rfcx-guardian-android
=====================

A collection of inter-dependent Android applications which, together, operate as autonomous Rainforest Connection guardians.

## Development environment

### Requirements

- [Android Studio](https://developer.android.com/studio) 3.2 or above
- (Optional: unless you need to recompile the native libs) Android SDK Tools: LLDB, CMake, NDK

### Getting started

#### Get the code

- Checkout the repo from GitHub
- Open in Android Studio
- Perform a Gradle sync (File -> Sync Project with Gradle) and accept licenses/install SDK packages (if prompted by Android Studio)

#### Run the setup role

You will need to run the setup role to register your guardian with the RFCx server:

1. Connect your phone.
2. In Android Studio, press "Run" and select the "setup" app.
3. You should be able to select your phone for running the app (instead of an emulator).
4. On the app, press the “Preferences” button
5. Set the api_url_base = `https://api.rfcx.org`
6. Set the install_api_registration_token = `162G1SS8`
7. Press the “Register” button
8. Verify that the guardian has got a new guid by opening the preferences and checking `guardian_guid`
9. Also, login to the Console -> Sites -> RFCx Lab -> check that you can see your phone (you will see the first 4 chars of the id)

After this is complete, you do not need the setup role and can safely delete it.

#### Run the guardian role

1. In Android Studio, press "Run" and select the "guardian" app.
2. Connect and choose your phone when prompted.
3. The first time you run, you will need to quit the app, open the Android settings on your phone and enable all the permissions for the guardian app.
4. Run again in Android Studio.
5. Open "Logcat" in Android Studio and observe the logs for "Rfcx". You should see the Audio Capture service begin immediately, and after 90 seconds you should see the Checkin service upload the audio to the server. At this point you can log into the RFCx console and listen to the latest audio from your phone.

### Run the admin role

You will need a rooted phone to test the admin role.

## Instructions for Orange Pi 3G-IoT

### Flash Android on Orange Pi (from Windows)

1. You need to download and install these tools
    1. [MTK Driver Installer](https://mega.nz/#F!WGwUhAZJ!xcc_4wd_UG_0OLruixz3ww!rGhSzJBL).
       Choose WIN8 if you are using Windows 8 or above.
       (Some users report failures during install -- [this is the solution](https://youtu.be/3g542NRQFwg))
    2. [MTK Flash Tool](https://mega.nz/#F!WGwUhAZJ!xcc_4wd_UG_0OLruixz3ww!mCJG3DgT)
    3. Orange Pi Android image: [IoT03_b1258_32g4g_ry_smt_v1.1.rar](https://mega.nz/#F!q8xQXZBQ!CXsQgfR2JaFsttTtBT_GMQ!3h5DTAJR)
2. Open the MTK Flash Tool *(flash_tool)*
    1. Make sure the Orange Pi is NOT connected.
    2. Choose the scatter-loading file **MT6572_Android_scatter** *(This is in the IoT03_b1258_32g4g_ry_smt_v1.1 folder after extracting the image.)*
    3. If this is the first time flashing the device then select "Download only". If you have flashed it before then select "Format all and download".
    4. Click "Download".
    5. Connect the Orange Pi to your computer using micro-usb cable into the Orange Pi and another side plug into your computer. *(The download process will begin after connecting the Orange Pi)*
    6. Disconnect the Orange Pi micro-usb cable after finishing.

### Share Orange Pi screen using Vysor

Before following the instruction below. You need to download Vysor first.

- Google Chrome Extension: https://chrome.google.com/webstore/detail/vysor/gidgenkbbabolejbgbpnhbimgjbffefm
- Normal program: https://www.vysor.io/download/

1. After you flashed the Orange Pi and disconnect the usb cable. Please make sure that you remove jumper cap from COM2 and the arrange jumper cap of COM1 as shown in the picture. *(the middle iron of jumper cap also must be the same as in the picture)*

![](docs/images/vysor1.PNG?raw=true)

2. Connect usb cable to your computer same as when flashing the device.
3. Once you connect to the computer, the green light led will light up for 5-6 seconds.
4. If the green led has disappeared, you need to press the power button until the green led light up again.
5. This process will take 5-8 minutes if it is first time boot up.
6. If you do these correctly the device will be appear in the Vysor.

![](docs/images/vysor2.PNG?raw=true)

7. Click view to show the Orange Pi screen

### Run the application through Android Studio

1. Connect usb cable to your computer same as the flashing.
2. Open Android Studio
3. Click the RUN button to see if the device connected. *(If Vysor sees the device, Android Studio will see too.)*

![](docs/images/androidstudio1.PNG?raw=true)

4. If the device connected, click on the device and OK button.
5. You should also see the device running in Vysor and receive the logcat messages on the Android Studio.

![](docs/images/androidstudio2.PNG?raw=true)

### How to root your Orange Pi

1. First, you need to download [KingoRoot app](https://www.kingoapp.com/android-root/download.htm) (for Windows)
2. Before rooting your Orange Pi, make sure that it is connected to your PC and USB debugging is on.
3. Start KingoRoot app. *(If it detects Orange Pi, it will show the **ROOT** button)*
4. Press the root button to start the rooting process. You can look the progression on the KingoRoot or on your Orange Pi's screen *(through Vysor)*
6. After the rooting process is success, your Orange Pi's screen will show the SuperUser app *(automatically installed)* with "Rooted" green text.
7. If it tells the device is not rooted yet, you need to re-do on **3** again until it is success.


