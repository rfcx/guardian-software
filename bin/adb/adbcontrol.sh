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
	echo "Example Actions:";
	echo "adbcontrol.sh [reboot, screenshot, software_update, airplanemode_toggle, clock_sync]";
	echo "adbcontrol.sh ping [all, instructions, etc]";
	echo "adbcontrol.sh identity_set [key] [value]";

	echo "";
	exit
	
elif [ "$CNTL" = "identity_set" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/identity_set/$KEY%7C$VAL;

elif [ "$CNTL" = "ping" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/ping/$KEY;

elif [ "$CNTL" = "gpio_set" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/gpio_set/$KEY%7C$VAL;

elif [ "$CNTL" = "software_update" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.updater/control/software_update;

else

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/$CNTL

fi



