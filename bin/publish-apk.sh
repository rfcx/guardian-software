#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

export ENV="production";

read -p "Please provide the database password for the '$ENV' environment: " -n 30 -r
export DB_PSWD="${REPLY}";

export APK_VERSION=`cat $SCRIPT_DIR/../rfcx-guardian-role-$ROLE/AndroidManifest.xml | grep 'android:versionName=' | cut -d'"' -f 2`;

export ALL_ROLES=`ls -l $SCRIPT_DIR/../ | grep "rfcx-guardian-role-" | cut -d' ' -f 14 | cut -d'-' -f 3`;

$SCRIPT_DIR/build-apk.sh $ROLE;

echo "generating sha1 digest...";
export SHA1=`openssl dgst -sha1 $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;

echo "copying apk to s3...";
aws s3 cp $SCRIPT_DIR/../tmp/$ROLE-$APK_VERSION.apk s3://rfcx-install/rfcx-guardian/guardian-android-$ROLE/$ENV/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers; 

export DB_URI=`cat $SCRIPT_DIR/_private/rfcx-api-db-uri-$ENV.txt;`;
export DB_USER="rfcx";
export DB_DATABASE="rfcx_api";

export ROLE_FROM_SQL=`ssh rfcx-proxy "mysql -h$DB_URI -u$DB_USER -p $DB_DATABASE -e \"SELECT id FROM GuardianSoftware WHERE role='$ROLE' LIMIT 1;\";";`;
export ROLE_ID=`echo $ROLE_FROM_SQL | cut -d' ' -f 2`;
export VERSION_INSERT_QUERY="INSERT INTO GuardianSoftwareVersions SET software_role_id=$ROLE_ID, version='$APK_VERSION', sha1_checksum='$SHA1', url='http://install.rfcx.org/rfcx-guardian/guardian-android-$ROLE/$ENV/$ROLE-$APK_VERSION.apk', is_available=1, release_date=NOW(), created_at=NOW(), updated_at=NOW();";
ssh rfcx-proxy "mysql -h$DB_URI -u$DB_USER -p $DB_DATABASE -e \"$VERSION_INSERT_QUERY\";"

export VERSION_ID_FROM_SQL=`ssh rfcx-proxy "mysql -h$DB_URI -u$DB_USER -p $DB_DATABASE -e \"SELECT id FROM GuardianSoftwareVersions WHERE sha1_checksum='$SHA1' LIMIT 1;\";";`;
export VERSION_ID=`echo $VERSION_ID_FROM_SQL | cut -d' ' -f 2`;
export UPDATE_ROLE_SQL="UPDATE GuardianSoftware SET current_version_id=$VERSION_ID WHERE id=$ROLE_ID;";
ssh rfcx-proxy "mysql -h$DB_URI -u$DB_USER -p $DB_DATABASE -e \"$UPDATE_ROLE_SQL\";"

cd $SCRIPT_DIR/../;

