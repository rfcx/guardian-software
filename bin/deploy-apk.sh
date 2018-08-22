#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

if [[ $ROLE = 'all' ]]
then
  $SCRIPT_DIR/_for-all-roles.sh deploy
else

  export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;

#  $SCRIPT_DIR/build-apk.sh $ROLE;

  export ADB_BIN="$ANDROID_HOME/platform-tools/adb";

  echo "transferring apk to device...";
  $ADB_BIN push $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  echo "restarting adbd with root access...";
  $ADB_BIN root; sleep 2;

  echo "killing app role process...";
  # $ADB_BIN shell 'kill $(ps | grep org.rfcx.guardian.$ROLE | cut -d " " -f 5);';

  echo "performing installation...";
  # $ADB_BIN shell pm set-install-location 1;
  $ADB_BIN shell pm install -f -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  echo "deleting apk...";
  $ADB_BIN shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

  # echo "waking up device..."
  # $ADB_BIN shell "input keyevent KEYCODE_POWER";

  echo "force relaunch of app role...";
  $ADB_BIN shell am start -n org.rfcx.guardian.$ROLE/$ROLE.activity.MainActivity;

  # echo "restarting adbd without root access..."
  # $ADB_BIN shell "setprop service.adb.root 0 && setprop ctl.restart adbd;";

  cd $SCRIPT_DIR/../;

fi
