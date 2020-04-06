#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export RW=$1;
export KEY=$2;
export VAL=$3;

if [ "$RW" = "r" ]; then

	adb shell content query --uri content://org.rfcx.guardian.guardian/prefs;

elif [ "$RW" = "w" ]; then

	adb shell content query --uri content://org.rfcx.guardian.guardian/prefs_set/$KEY%7C$VAL;
fi





