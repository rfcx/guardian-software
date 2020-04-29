#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;

export ENV="$2"; # staging or production;

read -p "Please provide the database password for the '$ENV' environment: " -n 50 -r
export DB_PSWD="${REPLY}";

export PROJECT_DIR="$SCRIPT_DIR/../..";
export ROLE_DIR="$PROJECT_DIR/role-$ROLE";

cd $PROJECT_DIR;

export APK_VERSION=`cat $ROLE_DIR/build.gradle | grep ' versionName ' | cut -d'"' -f 2`;

# echo ""; echo "RFCx $ROLE ($APK_VERSION)";

$SCRIPT_DIR/build-apk.sh $ROLE;

echo "generating sha1 digest...";
export SHA1=`openssl dgst -sha1 $PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;
echo $SHA1
echo "copying apk to s3...";
aws s3 cp $PROJECT_DIR/tmp/$ROLE-$APK_VERSION.apk s3://rfcx-install/rfcx-guardian/guardian-android-$ROLE/$ENV/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers; 

export DB_URI=`cat $PROJECT_DIR/bin/_private/rfcx-api-db-uri-$ENV.txt;`;
export DB_USER=`cat $PROJECT_DIR/bin/_private/rfcx-api-db-user-$ENV.txt;`;
export DB_DATABASE="rfcx_api";

export ROLE_FROM_SQL=`mysql -h$DB_URI -u$DB_USER -p$DB_PSWD $DB_DATABASE -e "SELECT id FROM GuardianSoftware WHERE role='$ROLE' LIMIT 1;";`;
export ROLE_ID=`echo $ROLE_FROM_SQL | cut -d' ' -f 2`;
export VERSION_INSERT_QUERY="INSERT INTO GuardianSoftwareVersions SET software_role_id=$ROLE_ID, version='$APK_VERSION', sha1_checksum='$SHA1', url='http://install.rfcx.org/rfcx-guardian/guardian-android-$ROLE/$ENV/$ROLE-$APK_VERSION.apk', is_available=1, release_date=NOW(), created_at=NOW(), updated_at=NOW();";
mysql -h$DB_URI -u$DB_USER -p$DB_PSWD $DB_DATABASE -e "$VERSION_INSERT_QUERY";

export VERSION_ID_FROM_SQL=`mysql -h$DB_URI -u$DB_USER -p$DB_PSWD $DB_DATABASE -e "SELECT id FROM GuardianSoftwareVersions WHERE sha1_checksum='$SHA1' LIMIT 1;";`;
export VERSION_ID=`echo $VERSION_ID_FROM_SQL | cut -d' ' -f 2`;
export UPDATE_ROLE_SQL="UPDATE GuardianSoftware SET current_version_id=$VERSION_ID WHERE id=$ROLE_ID;";
mysql -h$DB_URI -u$DB_USER -p$DB_PSWD $DB_DATABASE -e "$UPDATE_ROLE_SQL";

cd $PROJECT_DIR/../;

