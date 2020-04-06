#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

$SCRIPT_DIR/build-apk.sh $ROLE;

export PROJECT_DIR="$SCRIPT_DIR/..";
export ROLE_DIR="$PROJECT_DIR/$ROLE";

export APK_VERSION=`cat $ROLE_DIR/build.gradle | grep ' versionName ' | cut -d'"' -f 2`;

export ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb";

echo "transferring apk to device...";
$ADB_BIN push $PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

# echo "restarting adbd with root access...";
# # $ADB_BIN root; sleep 2;

# echo "killing app role process...";
# # $ADB_BIN shell 'kill $(ps | grep org.rfcx.guardian.$ROLE | cut -d " " -f 5);';

echo "performing installation...";
# $ADB_BIN shell pm set-install-location 1;
$ADB_BIN shell pm install -f -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "deleting apk...";
$ADB_BIN shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

# echo "waking up device..."
# $ADB_BIN shell "input keyevent KEYCODE_POWER";

echo "force relaunch of app role...";
$ADB_BIN shell am start -n org.rfcx.guardian.$ROLE/org.rfcx.guardian.$ROLE.activity.MainActivity;

# echo "restarting adbd without root access..."
# $ADB_BIN shell "setprop service.adb.root 0 && setprop ctl.restart adbd;";

cd $SCRIPT_DIR/../;

