#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export ROLE=$1;

$SCRIPT_DIR/build-apk.sh $ROLE;

export PROJECT_DIR="$SCRIPT_DIR/../..";
export ROLE_DIR="$PROJECT_DIR/role-$ROLE";

export APK_VERSION=`cat $ROLE_DIR/build.gradle | grep ' versionName ' | cut -d'"' -f 2`;


echo "transferring apk to device...";
$ADB push $PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

# echo "restarting adbd with root access...";
# # $ADB root; sleep 2;

# echo "killing app role process...";
# # $ADB shell 'kill $(ps | grep org.rfcx.guardian.$ROLE | cut -d " " -f 5);';

echo "performing installation...";
# $ADB shell pm set-install-location 1;
$ADB shell pm install -f -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "deleting apk...";
$ADB shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

# echo "waking up device..."
# $ADB shell "input keyevent KEYCODE_POWER";

echo "force relaunch of app role...";
$ADB shell am start -n org.rfcx.guardian.$ROLE/org.rfcx.guardian.$ROLE.activity.MainActivity;

# echo "restarting adbd without root access..."
# $ADB shell "setprop service.adb.root 0 && setprop ctl.restart adbd;";

cd $SCRIPT_DIR/../../;

