# RFCx Guardian software

A collection of Android applications which, together, operate as autonomous Rainforest Connection guardians.

## Development environment

### Requirements

- [Android Studio](https://developer.android.com/studio) 3.2 or above
- (Optional: unless you need to recompile the native libs) Android SDK Tools: LLDB, CMake, NDK

### Getting started

#### Get the code

- Checkout the repo from GitHub
- Open in Android Studio
- Perform a Gradle sync (File -> Sync Project with Gradle) and accept licences/install SDK packages (if prompted by Android Studio)

#### Run the guardian role

1. Connect your phone.
2. In Android Studio, press "Run" and select the "guardian" app.
3. You should be able to select your phone (or Orange Pi) for running the app (instead of an emulator).
4. (If you are on Android 8 or above then...) The first time you run, you will need to quit the app, open the Android settings on your phone and enable all the permissions for the guardian app. Then run the app again from Android Studio.
5. On the app, press the “Preferences” button
6. Set the api_url_base = `https://api.rfcx.org` (or to the staging server)
7. Login (make sure your user account has guardianCreator role) and register the guardian -- todo: add some screenshots
8. Verify that the guardian has got a new guid by opening the preferences and checking `guardian_guid`
9. Also, login to the Console -> Sites -> RFCx Lab -> check that you can see your phone (you will see the first 4 chars of the id)
10. Open "Logcat" in Android Studio and observe the logs for "Rfcx". You should see the Audio Capture service begin immediately, and after 90 seconds you should see the Checkin service upload the audio to the server. At this point you can log into the RFCx console and listen to the latest audio from your phone.

### Run the admin role

You will need a rooted phone to test the admin role. It will run without a rooted phone, but many of the functions (e.g. time sync, reboot) will not operate.


## Instructions for Orange Pi 3G-IoT

Before, starting any instructions here. Please read the information below and do it properly.

1. On the Orange Pi board there are two LEDs, red and green. **Red** indicates the **power** is connected and **green** indicates that the device is **ON**. Make sure that the power that connected to the Orange Pi is **5V**. If not the LEDs brightness will be low and cannot start the operating system.
2. By default the Orange Pi comes with both COM 1 jumpers in the correct position. No need to ever change them it seems. COM 2 jumper should be ON only when flashing the device (the rest of the time it should be OFF/removed).
2. The Orange Pi do not come with the **IMEI number**. You need to set it by yourself following the instructions below. Before install any roles, you should set the **IMEI number first**.
3. The Orange Pi is not rooted by default. You can root it by yourself following the instruction below.
4. Before using any roles, make sure there is the internet connection.
5. **Time and date** will not be correct on the first time you start the device. You need to set it to the present in **Setting Menu** or use the *Sntp service* on the **admin role** *(you need to root Orange Pi first)*
7. The Orange Pi comes with USB debugging **enabled** by default.
8. The Orange Pi comes with **auto allow the permission** (because it is Android 4.4).
9. The guardian role will capture audio and send to server automatically if:
    1. The date/time is (reasonably) close to current time
    2. **SD card** is connected *(any size is okay -- tested up to 64GB)*
    3. There is internet connection
    4. You don't need to press the **start button**. It will start the service automatically. You can also check the status in the app.

### Step 1: Flash Android on Orange Pi (from Windows)

1. Make sure COM 2 jumper is ON.
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
    
### Step 2: Share Orange Pi screen using Vysor

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

(Debugging: green led means powered off, red led means powered on.)

### Step 3: Set the IMEI number

1. Make sure that COM 2 jumper is OFF (removed).
2. First download [IMEI Writer](https://mega.nz/#F!WGwUhAZJ!xcc_4wd_UG_0OLruixz3ww!fCJmCTAY) (for Windows)
3. Extract and open **SN Write** in **SN_Writer_Tool_exe_v1.1716.00** directory
4. Choose **USB VCOM** and **Smart Phone**
5. Open System Config
    1. In **Write Option** choose **IMEI**
    2. In **IMEI Option** and **Header Option**, choose nothing
    3. In **MD1_DB** choose **BPLGUInfoCustomAppSrcP_MT6572_S00_MOLY_WR8_W1315_MD_WG_MP_V47_1_wg_n** in *IoT03L_b1258_wg_4g2g_ry_smt_v1.1\modem* (directory of android image)
    4. In **AP_DB** choose **APDB_MT6572_S01_KK1.MP7_** in the same directory
    5. In the newer version There are checkboxes called **"Load AP DB from DUT"** and **"Load modem DB from DUT"**. Make sure that you uncheck these two box before the next step.
    6. Save and then Start
    7. Put the IMEI Number with 15 digit
6. Make sure that Orange Pi do not connect to PC.
7. Click OK and then connect Orange Pi to PC immediately.
8. In Vysor, go to Settings -> About Phone -> Status and scroll down to verify that IMEI number you entered during the flashing process is shown there. (If there is an error then repeat step 5).

![](docs/images/checkimei.png?raw=true)

### Step 4: Root the Orange Pi

1. First, you need to download [KingoRoot app](https://www.kingoapp.com/android-root/download.htm) (for Windows)
2. Before rooting your Orange Pi, make sure that it is connected to your PC and USB debugging is on.
3. Start KingoRoot app. *(If it detects Orange Pi, it will show the **ROOT** button)*
4. Press the root button to start the rooting process. You can look the progression on the KingoRoot or on your Orange Pi's screen *(through Vysor)*
6. After the rooting process is success, your Orange Pi's screen will show the SuperUser app *(automatically installed)* with "Rooted" green text.
7. If it tells the device is not rooted yet, you need to re-do on **3** again until it is success.

### Step 5: Uninstall unused apps

1. Make sure the Orange Pi is the **only one** Android device that connected.
2. The script for uninstall is in **/script/delete-app/**
3. Click on (execute) **delete_unused_app_script.bat** and wait for apps uninstalled.

### Step 6: Run the application through Android Studio

1. Make sure the Orange Pi is connected.
2. Open Android Studio
3. Click the "Run" button to see if the device connected. *(If Vysor sees the device, Android Studio should detect it too.)*

![](docs/images/androidstudio1.PNG?raw=true)

4. If the device connected, click on the device and OK button.
5. You should also see the device running in Vysor and receive the logcat messages on the Android Studio.

![](docs/images/androidstudio2.PNG?raw=true)


