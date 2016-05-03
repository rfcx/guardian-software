#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

if [[ $ROLE = 'all' ]]
then
  $SCRIPT_DIR/_for-all-roles.sh build
else

  export KEY_PSWD=`cat $SCRIPT_DIR/_private/rfcx-guardian-keystore-pswd.txt;`;

  cd $SCRIPT_DIR/../;

  if [ ! -d tmp ]; then
    echo "creating tmp directory";
    mkdir tmp;
  fi

  cd rfcx-guardian-role-$ROLE;

  export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;
  echo "RFCx $ROLE ($APK_VERSION)";

  echo "generating build configuration...";
  export BUILD_CONFIG=`android update project -p . -n rfcx-guardian-role-$ROLE`;

  echo "key.store=$SCRIPT_DIR/_private/rfcx-guardian-keystore.jks" >> local.properties;
  echo "key.alias=rfcx-guardian-android" >> local.properties;
  echo "key.store.password=$KEY_PSWD" >> local.properties;
  echo "key.alias.password=$KEY_PSWD" >> local.properties;

  echo "android.library.reference.1=../rfcx-guardian-lib-utility" >> local.properties;

  echo "setting up build process...";
  export ANT_CLEAN=`ant clean`;

  echo "building apk...";
  export APK_PATH=`ant release | grep 'Release Package:' | cut -d':' -f 2 | cut -d' ' -f 2`;

  echo "copying apk into tmp directory..."
  cp $APK_PATH ../tmp/$ROLE-$APK_VERSION.apk;

  export POST_CLEANUP=`cd $SCRIPT_DIR/../rfcx-guardian-role-$ROLE; rm local.properties build.xml proguard-project.txt;`;

  cd $SCRIPT_DIR/../;

fi
