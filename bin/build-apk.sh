#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export KEY_PSWD=`cat $SCRIPT_DIR/private/rfcx-guardian-build.txt;`;

cd $SCRIPT_DIR/../;

if [ ! -d tmp ]; then
  echo "creating tmp directory";
  mkdir tmp;
fi

cd rfcx-guardian-$ROLE;

export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;
echo "RFCx $ROLE ($APK_VERSION)";

echo "generating build configuration...";
export BUILD_CONFIG=`android update project -p . -n rfcx-guardian-$ROLE`;

echo "key.store=$SCRIPT_DIR/private/rfcx-guardian.jks" >> local.properties;
echo "key.alias=rfcx-guardian-android-$ROLE" >> local.properties;
echo "key.store.password=$KEY_PSWD" >> local.properties;
echo "key.alias.password=$KEY_PSWD" >> local.properties;

echo "setting up build process...";
export ANT_CLEAN=`ant clean`;

echo "building apk...";
export APK_PATH=`ant release | grep 'Release Package:' | cut -d':' -f 2 | cut -d' ' -f 2`;

echo "copying apk into tmp directory..."
cp $APK_PATH ../tmp/$ROLE-$APK_VERSION.apk;

export POST_CLEANUP=`cd $SCRIPT_DIR/../rfcx-guardian-$ROLE; rm local.properties build.xml proguard-project.txt;`;

cd $SCRIPT_DIR/../;
