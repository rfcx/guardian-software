#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
cd $SCRIPT_DIR/../;

if [ ! -d tmp ]; then
  echo "creating tmp directory";
  mkdir tmp;
fi

#cd ~/Downloads/;
#adb push 0.4.72.apk /data/data/org.rfcx.guardian.updater/files/0.4.72.apk; adb shell pm install -r /data/data/org.rfcx.guardian.updater/files/0.4.72.apk; adb shell rm /data/data/org.rfcx.guardian.updater/files/0.4.72.apk;