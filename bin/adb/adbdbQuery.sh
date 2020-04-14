#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export ROLE=$1;
export CATEGORY=$2;
export DB=$3;
export CMD=$4;

if [ "$ROLE" = "help" ]; then

	echo "";
	echo "This allows you to interact simply with the sqlite databases within the RFCx roles.";
	echo "Usage Guide:";
	echo "";
	echo "List Role Databases:";
	echo "Usage: adbdbQuery.sh [ROLE] list";
	echo "Example: adbdbQuery.sh admin list";
	echo "";
	echo "Count Rows:";
	echo "Usage: adbdbQuery.sh [ROLE] [CATEGORY] [DATABASE] count";
	echo "Example: adbdbQuery.sh guardian checkin queued count";
	echo "";
	echo "Print Row Content (all rows):";
	echo "Usage: adbdbQuery.sh [ROLE] [CATEGORY] [DATABASE] all";
	echo "Example: adbdbQuery.sh admin screenshots captured all";
	echo "";
	echo "Truncate/Clear a database:";
	echo "Usage: adbdbQuery.sh [ROLE] [CATEGORY] [DATABASE] truncate";
	echo "Example: adbdbQuery.sh admin screenshots captured truncate";

	echo "";
	exit
fi

if [ "$CATEGORY" = "list" ]; then
	CMD="$CATEGORY";
fi

export DB_DIR="/data/user/0/org.rfcx.guardian.$ROLE/databases";

echo "";

if [ "$CMD" = "list" ]; then
	echo "Databases:";
	$ADB shell "ls $DB_DIR" | grep -v "journal"

elif [ "$CMD" = "count" ]; then
	echo "$CATEGORY-$DB: Row Count:";
	$ADB shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT COUNT(*) FROM $DB;'";

elif [ "$CMD" = "all" ]; then
	echo "$CATEGORY-$DB: Rows:";
	$ADB shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT COUNT(*) FROM $DB;'";
	$ADB shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT * FROM $DB;'";

elif [ "$CMD" = "truncate" ]; then
	$ADB shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'DELETE FROM $DB;'";

fi

echo "";