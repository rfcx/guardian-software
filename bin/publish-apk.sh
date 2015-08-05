#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export APK_VERSION=$2;

cd $SCRIPT_DIR/../rfcx-guardian-$ROLE/;

export SHA1=`openssl dgst -sha1 ../tmp/$ROLE-$APK_VERSION.apk | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;

aws s3 cp ../tmp/$ROLE-$APK_VERSION.apk s3://rfcx-static/dl/guardian-android-$ROLE/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers; 

$SCRIPT_DIR/publish-db-entry.sh $ROLE $APK_VERSION $SHA1;

cd $SCRIPT_DIR/../;
