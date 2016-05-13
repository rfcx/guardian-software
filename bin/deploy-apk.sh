#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

if [[ $ROLE = 'all' ]]
then
  $SCRIPT_DIR/_for-all-roles.sh deploy
else

  export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;

  $SCRIPT_DIR/build-apk.sh $ROLE;

  echo "transferring apk to device...";
  adb push $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  echo "killing app role process...";
  adb shell 'kill $(ps | grep org.rfcx.guardian.$ROLE | cut -d " " -f 5); kill $(ps | grep org.rfcx.guardian.$ROLE | cut -d " " -f 5);';

  echo "performing installation...";
  adb shell pm install -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  echo "deleting apk...";
  adb shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  echo "force relaunch of app role";
  adb shell am start -n org.rfcx.guardian.$ROLE/org.rfcx.guardian.$ROLE.activity.MainActivity;

  cd $SCRIPT_DIR/../;

fi
