#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export RW=$1;
export KEY=$2;
export VAL=$3;

if [ "$RW" = "r" ]; then

	if [ "$KEY" = "all" ]; then
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs;
	else
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs/$KEY;
	fi

elif [ "$RW" = "w" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs_set/$KEY%7C$VAL;
fi





