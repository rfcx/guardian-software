#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export APK_VERSION=$2;

cd $SCRIPT_DIR/../rfcx-guardian-$ROLE/;

export SHA1=`openssl dgst -sha1 ../tmp/$ROLE-$APK_VERSION.apk | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;

aws s3 cp ../tmp/$ROLE-$APK_VERSION.apk s3://rfcx-static/dl/guardian-android-$ROLE/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers; 

echo "";
echo "INSERT INTO GuardianSoftware SET role='$ROLE', number='$APK_VERSION', sha1_checksum='$SHA1', url='http://static.rfcx.org/dl/guardian-android-$ROLE/$ROLE-$APK_VERSION.apk', is_available=1, release_date=NOW(), created_at=NOW(), updated_at=NOW();";
echo "";

cd $SCRIPT_DIR/../;
