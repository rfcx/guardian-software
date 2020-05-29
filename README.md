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

#### Build Signed APKs (need Android Studio installed)

1. First need to prepare necessery files located in /gradle/signing/
   - rfcx-guardian-keystore.jks
   - rfcx-guardian-keystore-alias.txt
   - rfcx-guardian-keystore-password.txt
   - rfcx-platform-keystore.jks
   - rfcx-platform-keystore-alias.txt
   - rfcx-platform-keystore-password.txt
2. Use this command after prepared

   For specific role

   ```
   gradlew :role:assembleRelease
   ```
   
   For all roles
   
   ```
   gradlew assembleRelease
   ```
   
3. Output will be in /{role}/build/outputs/apk/release/

#### Download apps from script

1. If your Environment variables do not have **ANDROID_SDK_ROOT**, create it and point to android-sdk root path
2. Run script with version **download-apk-install.sh 0.x.x** in /bin/

#### Run the guardian role

1. Connect your phone.
2. In Android Studio, press "Run" and select the "guardian" app.
3. You should be able to select your phone (or Orange Pi) for running the app (instead of an emulator).
4. (If you are on Android 8 or above then...) The first time you run, you will need to quit the app, open the Android settings on your phone and enable all the permissions for the guardian app. And enable the Location Sharing(GPS). Then run the app again from Android Studio.
5. Login (make sure your user account has guardianCreator role). Once logined, copy a login code and paste on an entry section.
6. Register the guardian
7. If registration success, guid will show up on the screen.
8. Also, login to the Console/Dashboard -> Sites -> RFCx Lab -> check that you can see your phone (you will see the first 4 chars of the id)
9. Open "Logcat" in Android Studio and observe the logs for "Rfcx". You should see the Audio Capture service begin immediately, and after 90 seconds you should see the Checkin service upload the audio to the server. At this point you can log into the RFCx console and listen to the latest audio from your phone.

### Run the admin role

You will need a rooted phone to test the admin role. It will run without a rooted phone, but many of the functions (e.g. time sync, reboot) will not operate.

## Instructions for Orange Pi 3G-IoT

Before getting started, key points to be familiar with:

1. On the Orange Pi board there are two LEDs, red and green. **Red** indicates the **power** is connected and **green** indicates that the device is **ON**. Make sure that the power that connected to the Orange Pi is **5V**. If not the LEDs brightness will be low and cannot start the operating system.
2. By default the Orange Pi comes with both COM 1 jumpers in the correct position. No need to ever change them it seems. COM 2 jumper should be ON only when flashing the device (the rest of the time it should be OFF/removed).
3. The OrangePi does not come with a pre-set **IMEI number**. You will need to set it yourself by following the instructions below.
4. Before using any roles, make sure there is the internet connection.
5. **Timezone automatically** need to uncheck so that Admin role can change it.
6. **Default Write Disk** need to set to **Phone Storage** although sd card is installed.
7. **Default SMS app** need to set to Admin role.
8. The OrangePi comes with USB debugging **enabled** by default.
9. The OrangePi comes with **auto allow the permission** (because it is Android 4.4.2).
10. The guardian role will capture audio and send to server automatically if:
    1. The date/time is (reasonably) close to current time
    2. Guardian is registered
    3. **SD card** is connected *(any size is okay -- tested up to 64GB)*
    4. There is internet connection
    5. You don't need to press the **start button**. It will start the service automatically. You can also check the status in the app.

#### Install apps

You can install apps by 2 ways
1. Using script in /bin/
   1. Your **PATH** should have Android SDK root path or If you have Android Studio installed then your Environment Variables also should have **ANDROID_SDK_ROOT**. If there is none of those two then you need to set it yourself.
   2. Connect your OrangePi to your PC via usb port
   3. Run script **download-apk-install.sh 0.x.x**
   4. All apps will be installed and start automatically
2. Using Android Studio
   1. Connect your OrangePi to your PC via usb port
   2. In Android Studio, select the **guardian|admin|updater** app and press **Run**.
   3. The app will be installed and start automatically

#### Run the guardian role

1. Login (make sure your user account has guardianCreator role). Once logined, the browser will be redirected to the app automatically.
2. Register the guardian.
3. If registration success, guid will show up on the screen and the audio capture service will start.
4. Also, login to the Console/Dashboard -> Sites -> RFCx Lab -> check that you can see your OrangePi (you will see the first 4 chars of the id)
5. Open **Logcat** in Android Studio and observe the logs for "Rfcx". You should see the Audio Capture service begin immediately, and after 90 seconds you should see the Checkin service upload the audio to the server. At this point you can log into the RFCx console and listen to the latest audio from your OrangePi.

#### Run the admin role

OrangePi has already been rooted so you do not need to do anything extra to run admin role.

### Step 1: Flash Android on OrangePi (using Windows)

1. Make sure COM 2 jumper is ON. (Note: in practice we found that the jumper can be OFF for the flashing process.)
1. You need to download and install these tools
    1. [MTK Driver Installer](https://drive.google.com/open?id=15GXkFqZ95ilu482SXAiOPlxm878otwu9).
       Choose WIN8 if you are using Windows 8 or above.
       (Some users report failures during install -- [this is the solution](https://youtu.be/3g542NRQFwg))
    2. [MTK Flash Tool](https://drive.google.com/open?id=1VXU-Jb5A8cFco3Q6uYht684pxjnp0pJd)
    3. Orange Pi Android image: [Android Image Downloads](https://drive.google.com/open?id=1N0C_qYKcDNs4J4nvJBg0qHr6xXDQj6Pz)
2. Open the MTK Flash Tool *(flash_tool)*
    1. Make sure the Orange Pi is NOT connected.
    2. Choose the scatter-loading file **MT6572_Android_scatter** *(This is in the Android_OS_for_Orange_Pi_3G-IoT/images/ folder after extracting the image.)*
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

Before you start, you will need to generate a suitable IMEI number. Please use this [Online IMEI Generator](https://static.rfcx.org/tools/imei/generator.html) to create a valid value.

1. Make sure that COM 2 jumper is OFF (removed).
2. First download [IMEI Writer](https://drive.google.com/open?id=1JBEbILcHHfWM3Yz4e4J9Yc5Leix9m1uI) (for Windows)
3. Extract and open **SN Write** in **SN_Writer_Tool_exe_v1.1716.00** directory
4. Choose **USB VCOM** and **Smart Phone**
5. Open System Config
    1. In **Write Option** choose **IMEI**
    2. In **IMEI Option** and **Header Option**, choose nothing
    3. In **MD1_DB** choose **BPLGUInfoCustomAppSrcP_MT6572_S00_MOLY_WR8_W1315_MD_WG_MP_V47_1_wg_n** in *Android_OS_for_Orange_Pi_3G-IoT\modem* (directory of android image)
    4. In **AP_DB** choose **APDB_MT6572_S01_KK1.MP7_** in the same directory
    5. In the newer version There are checkboxes called **"Load AP DB from DUT"** and **"Load modem DB from DUT"**. Make sure that you uncheck these two box before the next step.
    6. Save and then Start
    7. Put the IMEI Number with 15 digit
6. Make sure that Orange Pi do not connect to PC.
7. Click OK and then connect Orange Pi to PC immediately (be quick else the device will not be detected in time and you will have to disconnect/start again).
8. In Vysor, go to Settings -> About Phone -> Status and scroll down to verify that IMEI number you entered during the flashing process is shown there. (If there is an error then repeat step 5).

   ![](docs/images/checkimei.png?raw=true)

### Step 4: Run the application through Android Studio

1. Make sure the Orange Pi is connected.

2. Open Android Studio

3. Click the **Run** button to see if the device connected. *(If Vysor sees the device, Android Studio should detect it too.)*

   ![](docs/images/androidstudio1.PNG?raw=true)

4. If the device connected, click on the device and OK button.

5. You should also see the device running in Vysor and receive the logcat messages on the Android Studio.

   ![](docs/images/androidstudio2.PNG?raw=true)

### Step 5: How to debug OrangePi over Bluetooth instead of USB cable

1. Make sure Orange Pi is connected, Bluetooth is on and set visibility timeout to never time out as follows.
    1. Bluetooth is *on*

       ![](docs/images/step8-1a.png?raw=true)
    2. Make sure the device is *Visible to all nearby*

       ![](docs/images/step8-1b.png?raw=true)
    3. Open the *Visibility timeout* (bottom right menu)

       ![](docs/images/step8-1c.png?raw=true)
    4. Set the timeout to *Never time out*

       ![](docs/images/step8-1d.png?raw=true)
       
2. Open Terminal or Command prompt

   ```
   $ adb shell
   $ setprop persist.adb.tcp.port 4455
   ```

3. Take USB off and start running Orange Pi from other power resources.

4. Pair Orange Pi with Bluetooth from your machine

5. _For Windows_, go to **\Control Panel\Network and Internet\Network Connections**
    1. Double click on **Bluetooth Network Connection**
    2. Right click on Orange Pi *(probably named as IOT03)*
    3. Connect using > Access point *(if it is **directly connect**, re-pair Orange Pi and try again)* 
    4. Open Command Prompt
    5. Type `ipconfig` and copy **default gateway IP** of Bluetooth Network Connection *(probably 192.168.44.1)*
    6. Type `adb connect <IP>:4455` *(adb connect 192.168.44.1:4455)*. It will show **connected to IP:4455** if successful.

    _For Mac_, open Bluetooth from the System Preferences
    1. Find the device named "IoT03" and connect.
    2. Open Terminal, and run `ifconfig`. You should see an entry (probably `en3`) with `inet 192.168.44.160`.
    3. Type `adb connect 192.168.44.1:4455` and it will show `connected to ...` if successful.

6. Now you can `adb shell` or you can open Vysor.

7. If you want to go back to USB debugging, you need to `adb shell` and then enter `setprop persist.adb.tcp.port ""`.

### Step 6: How to debug OrangePi over Wifi Hotspot

1. Require guardian and admin role installed.

2. Start guardian role app.

3. Open **Preferences setting** at top right menu

4. Set **admin_enable_wifi** and **admin_enable_tcp_adb** to true

   ![](docs/images/pref_setting.PNG?raw=true)

5. Wifi hotspot will enable and tcp port will change to 7329. Wifi hotspot name will named as rfcx-{guid} and password is rfcxrfcx

6. Go to Settings > Wireless & Networks - **More...** > Tethering & portable hotspot > Wifi hotspot > Keep Wifi hotspot on > Change to **Always**

   ![](docs/images/wifi_hotspot.PNG?raw=true)

7. Then take USB cable off and power OrangePi with external power source

8. Connect your PC to OrangePi Wifi hotspot

9. Now you can debug your OrangePi using its IP (default is 192.168.43.1)

   ```
   adb connect 192.168.43.1:7329
   ```

10. Then you can see the screen using Vysor

### Step 7: How to set Default SMS app to Admin role

1. Require Admin role installed

2. Go to Settings > Wireless & Networks - **More...** > Default SMS app > Choose **RFCx Admin**

   ![](docs/images/sms_to_admin.PNG?raw=true)

### Step 8: How to set Default Write Disk to Phone storage

1. Go to Settings > Device - **Storage** > Choose Default Write Disk - **Phone storage**

   ![](docs/images/write_disk.PNG?raw=true)

### Step 9: How to set Timezone automatically to off

1. Go to Setting > System - **Date & Time** > Uncheck **Automatic time zone**

   ![](docs/images/timezone_off.PNG?raw=true)

### Step 10: How to connect i2c and load i2c module

1. First, place Orange Pi same position as in the image.

   ![](docs/images/i2c1.PNG?raw=true)

2. This will use the most right 10 pins of the board, so you should take the pin 11-12 off or bend it into the left side.

   ![](docs/images/i2c2.PNG?raw=true)

3. Put the power wires(pack of yellow and red wires) into the most right 10 pins by the red wire should be on the right side.

   ![](docs/images/i2c3.PNG?raw=true)

4. The step to start Orange Pi is the same as before.

5. You can debug OrangePi by using Bluetooth on [Step 5](https://github.com/rfcx/rfcx-guardian-android/tree/develop#step-5-how-to-debug-orangepi-over-bluetooth-instead-of-usb-cable) or [Step 6](https://github.com/rfcx/rfcx-guardian-android/tree/develop#step-6-how-to-debug-orangepi-over-wifi-hotspot)

### Step 7: How to setup, run and test the I2C

1. Plugin the sentinel power wires to the OrangePi on [Step 10](https://github.com/rfcx/rfcx-guardian-android/tree/develop#step-10-how-to-connect-i2c-and-load-i2c-module)

2. Install admin role

3. When the admin role starts, switch **monitor** to **ON** in the screen

4. The result will be monitored real-time and can stop by switch it to **OFF**

## Instructions for Android phone

There are differences for normal Android phone and OrangePi.
- It is not rooted by default.
- You need to enable permission for apps yourself.

#### Install apps

The process is same as OrangePi

#### Run the guardian role

The process is same as OrangePi but you need to enable Permission yourself by
- (If you are on Android 8 or above then...) The first time you run, you will need to quit the app, open the Android settings on your phone and enable all the permissions for the guardian app. And enable the Location Sharing(GPS) then run the app again.

#### Run the admin role

You will need a rooted phone to test the admin role. It will run without a rooted phone, but many of the functions (e.g. time sync, reboot) will not operate.
Also allow all permission is required.
