rfcx-guardian-android
=====================

A collection of inter-dependent Android applications which, together, operate as autonomous Rainforest Connection guardians.

## Development environment

### Requirements

- Android Studio 3.2+
- Android SDK Tools: LLDB, CMake, NDK

### Getting started

#### Get the code

- Checkout the repo from github
- Open in Android Studio

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

You might need a rooted phone to test the admin role.
