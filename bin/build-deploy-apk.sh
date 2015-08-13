#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;

$SCRIPT_DIR/build-apk.sh $ROLE;

echo "transferring apk to device...";
adb push $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "performing installation...";
adb shell pm install -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "deleting apk...";
adb shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

cd $SCRIPT_DIR/../;
