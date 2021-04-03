#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export RW=$1;
export KEY=$2;
export VAL=$3;

if [ "$RW" = "help" ]; then

	echo "";
	echo "This allows you to interact simply with the prefs content providers of the RFCx roles.";
	echo "Usage Guide:";
	echo "Usage: adbprefs.sh [READ/WRITE] [KEY] {[VALUE]}";
	echo "";
	echo "List All Prefs and Values:";
	echo "Usage: adbprefs.sh r all";
	echo "";
	echo "Read Single Pref Value:";
	echo "Usage: adbprefs.sh r [KEY]";
	echo "Example: adbprefs.sh r enable_audio_capture";
	echo "";
	echo "Update/Write Single Pref Value:";
	echo "Usage: adbprefs.sh w [KEY] [VALUE]";
	echo "Example: adbprefs.sh w api_checkin_port 1883";

	echo "";
	exit

elif [ "$RW" = "r" ]; then

	if [ "$KEY" = "all" ]; then
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs;
	else
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs/$KEY;
	fi

elif [ "$RW" = "w" ]; then

	if [ "$KEY" = "admin_system_timezone" ]; then
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs_set/$KEY%7C$VAL%2F$4;
	else 
		$ADB shell content query --uri content://org.rfcx.guardian.guardian/prefs_set/$KEY%7C$VAL;
	fi

fi





