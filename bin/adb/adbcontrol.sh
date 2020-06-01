#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export CNTL=$1;
export KEY=$2;
export VAL=$3;

if [ "$CNTL" = "help" ]; then

	echo "";
	echo "This allows you to interact simply with the control content providers of the RFCx roles.";
	echo "Usage Guide:";
	echo "Usage: adbcontrol.sh {[ACTION]}";
	echo "";
	echo "Allowed Actions:";
	echo "reboot kill relaunch screenshot logcat airplanemode_toggle airplanemode_enable sntp_sync";

	echo "";
	exit
	
elif [ "$CNTL" = "identity_set" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/identity_set/$KEY%7C$VAL;

elif [ "$CNTL" = "ping" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/ping/$KEY;

else

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/$CNTL

fi



