#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;

$SCRIPT_DIR/build-apk.sh $ROLE;

echo "generating sha1 digest...";
export SHA1=`openssl dgst -sha1 $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;

echo "copying apk to s3...";
aws s3 cp $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk s3://rfcx-static/dl/guardian-android-$ROLE/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers; 

export DB_URI=`cat $SCRIPT_DIR/private/rfcx-guardian-api-db-uri.txt;`;
#export DB_PSWD=`cat $SCRIPT_DIR/private/rfcx-guardian-api-db-pswd.txt;`;

ssh rfcx-proxy "mysql -h$DB_URI -uebroot -p ebdb -e \"INSERT INTO GuardianSoftware SET role='$ROLE', number='$APK_VERSION', sha1_checksum='$SHA1', url='http://static.rfcx.org/dl/guardian-android-$ROLE/$ROLE-$APK_VERSION.apk', is_available=1, release_date=NOW(), created_at=NOW(), updated_at=NOW();\";"

cd $SCRIPT_DIR/../;





echo "transferring apk to device...";
adb push $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "performing installation...";
adb shell pm install -r /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

echo "deleting apk...";
adb shell rm /data/local/tmp/rfcx-$ROLE-$APK_VERSION.apk;

cd $SCRIPT_DIR/../;
