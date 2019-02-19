rfcx-guardian-android
=====================

A collection of inter-dependent Android applications which, together, operate as autonomous Rainforest Connection guardians.

## Development environment

### Requirements

- Eclipse IDE (latest version from (Eclipse Foundation)[https://www.eclipse.org/downloads/]) - install the version "Eclipse IDE for Java Developers"
- Java SE 7 (JDK) - if you have Android Studio then you probably have this already

### Getting started

Importing the projects:

- Checkout the repo from github
- Open Eclipse and choose "Import existing projects"
- "Select root directory" is the root of the checked out repo, and check the option "Search for nested projects"
- You should see at least 10 projects, select them all except the root project and click "Finish"
- You will be prompted to install the Android (andmore) extension from Eclipse Marketplace (unless you already have it) which are required for building Android apps. After installation, Eclipse will restart.

Setting up the Android extension:

- If you have Android Studio installed then Eclipse should find the Android SDK location and use the same files -- this will save you having 2 copies of the Android SDK. However, I couldn't get the Android extension to work with the latest version of the Android SDK Tools, so...
- Download the old version of Android SDK Tools (2.5.2 was the last version with the command line tools like traceview):
  [Mac OS X](http://dl-ssl.google.com/android/repository/tools_r25.2.5-macosx.zip) or
  [Windows](http://dl-ssl.google.com/android/repository/tools_r25.2.5-windows.zip) or
  [Linux](http://dl-ssl.google.com/android/repository/tools_r25.2.5-linux.zip) 
- Unzip to a new folder, e.g. /Users/ant/Library/Android/sdk-old/tools
- In Eclipse, open the Preferences, go to Android. Change the SDK Location to: /Users/ant/Library/Android/sdk-old
- It will prompt you to open the SDK Manager, which you should do and install ONLY the following:
  - Android SDK Platform tools
  - Android SDK Build tools 25.0.3
  - Android 4.4.2 (API 19) -> SDK Platform
  - Android 4.4.2 (API 19) -> Intel x86 Atom Image

Building for first time:

- Ensure Eclipse is set to Java 7:
  - Preferences -> Java -> Compiler -> Compiler Compliance Level = 1.7
  -                     -> Installed JREs -> (Check) Java SE 7
- Clean, then check the rfcx-guardian-role-setup project is building correctly
- Eclipse will build automatically

Running:

- Open the Android perspective. Window -> Perspective -> Open Perspective -> Other... then select Android
- You will need to create an emulator for API 19 (unless you are running on device)
- Check there is an emulator or device running from "Device Management"
- Run the rfcx-guardian-role-setup project					  
