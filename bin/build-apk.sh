#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

if [[ $ROLE = 'all' ]]; then

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
  echo ""; echo "RFCx $ROLE ($APK_VERSION)";

  echo "generating build configuration...";
  export BUILD_CONFIG=`android update project -p . -n rfcx-guardian-role-$ROLE`;

  echo "key.store=$SCRIPT_DIR/_private/rfcx-guardian-keystore.jks" >> local.properties;
  echo "key.alias=rfcx-guardian-android" >> local.properties;
  echo "key.store.password=$KEY_PSWD" >> local.properties;
  echo "key.alias.password=$KEY_PSWD" >> local.properties;

  # include rfcx shared 'utility' library
  echo "android.library.reference.1=../rfcx-guardian-lib-utility" >> local.properties;

  # for the 'guardian' role, include rfcx shared audio encoding library
  if [[ $ROLE = 'guardian' ]]; then
    
    echo "android.library.reference.2=../rfcx-guardian-lib-audio" >> local.properties;
    cd $SCRIPT_DIR/../rfcx-guardian-lib-audio/jni && ndk-build;

  fi

  # for the 'admin' role, include rfcx shared i2c access library
  if [[ $ROLE = 'admin' ]]; then
    
 #   echo "android.library.reference.2=../rfcx-guardian-lib-i2c" >> local.properties;
    cd $SCRIPT_DIR/../rfcx-guardian-lib-i2c/jni && ndk-build;

    cp $SCRIPT_DIR/../rfcx-guardian-lib-i2c/libs/armeabi/i2cget $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/assets/i2cget;
#    cp $SCRIPT_DIR/../rfcx-guardian-lib-i2c/libs/armeabi/i2cset $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/assets/i2cset;
     
  fi

  cd $SCRIPT_DIR/../rfcx-guardian-role-$ROLE;

  echo "setting up build process...";
  export ANT_CLEAN=`ant clean`;

  echo "building apk...";
  export APK_PATH=`ant release | grep 'Release Package:' | cut -d':' -f 2 | cut -d' ' -f 2`;

  echo "copying apk into tmp directory..."
  cp $APK_PATH ../tmp/$ROLE-$APK_VERSION.apk;

  export POST_CLEANUP=`cd $SCRIPT_DIR/../rfcx-guardian-role-$ROLE; rm local.properties build.xml proguard-project.txt;`;

  cd $SCRIPT_DIR/../;

fi
