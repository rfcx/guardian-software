#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ROLE=$1;
export CATEGORY=$2;
export DB=$3;
export CMD=$4;

if [ "$CATEGORY" = "list" ]; then
	CMD="$CATEGORY";
fi

export DB_DIR="/data/data/org.rfcx.guardian.$ROLE/databases";

echo "";

if [ "$CMD" = "list" ]; then
	echo "Databases:";
	adb shell "ls $DB_DIR" | grep -v "journal"

elif [ "$CMD" = "count" ]; then
	echo "Rows:";
	adb shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT COUNT(*) FROM $DB;'";

elif [ "$CMD" = "all" ]; then
	echo "Rows:";
	adb shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT COUNT(*) FROM $DB;'";
	adb shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'SELECT * FROM $DB;'";

elif [ "$CMD" = "truncate" ]; then
	adb shell "sqlite3 $DB_DIR/$CATEGORY-$DB.db 'DELETE FROM $DB;'";

fi

echo "";