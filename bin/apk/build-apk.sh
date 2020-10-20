#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

export SDKVERSION="29.0.2";

export APKSIGNER_BIN="$ANDROID_SDK_ROOT/build-tools/$SDKVERSION/apksigner";

export KEYSTORE="$SCRIPT_DIR/../_private/rfcx-platform-keystore.jks";
export KEY_ALIAS=`cat $SCRIPT_DIR/../_private/rfcx-platform-keystore-alias.txt;`;
export KEY_PSWD=`cat $SCRIPT_DIR/../_private/rfcx-platform-keystore-pswd.txt;`;

export PROJECT_DIR="$SCRIPT_DIR/../..";
export ROLE_DIR="$PROJECT_DIR/role-$ROLE";

cd $PROJECT_DIR;

if [ ! -d $PROJECT_DIR/tmp ]; then
  echo "creating tmp directory";
  mkdir $PROJECT_DIR/tmp;
fi

export APK_VERSION=`cat $ROLE_DIR/build.gradle | grep ' versionName ' | cut -d'"' -f 2`;

export APK_TYPE="release"; #"debug";

echo ""; echo "RFCx $ROLE ($APK_VERSION)";

##
##	NDK native compilation has been left out as we migrate toward building with gradle.
##  We'll need to rewrite this later, as it becomes needed.
##

cd $PROJECT_DIR;

echo "building apk...";

if [[ "$APK_TYPE" = "debug" ]]; then
	$PROJECT_DIR/gradlew :role-$ROLE:assembleDebug;
elif [ "$APK_TYPE" = "release" ]; then
	$PROJECT_DIR/gradlew :role-$ROLE:assembleRelease;
fi

export APK_PATH_UNSIGNED="$ROLE_DIR/build/outputs/apk/$APK_TYPE/$ROLE-$APK_VERSION-$APK_TYPE-unsigned.apk";
export APK_PATH_SIGNED="$PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk";

if [ -f "$PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk" ]; then
	export DELETE_OLD_APK=`rm $PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk`;
fi
echo "signing apk and placing signed version into tmp directory..."
$APKSIGNER_BIN sign --ks $KEYSTORE --ks-key-alias $KEY_ALIAS --ks-pass pass:$KEY_PSWD --out $APK_PATH_SIGNED $APK_PATH_UNSIGNED;

export POST_CLEANUP=`rm $APK_PATH_UNSIGNED;`;

cd $SCRIPT_DIR/../../;

