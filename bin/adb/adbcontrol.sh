#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export CNTL=$1;
export KEY=$2;
export VAL=$3;
export XTRA=$4;

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

	$ADB shell content query --uri content://org.rfcx.guardian.guardian/ping/$KEY%7C$VAL;

elif [ "$KEY" = "asset_cleanup" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.$CNTL/control/asset_cleanup;

elif [ "$CNTL" = "keycode" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/keycode/$KEY;

elif [ "$CNTL" = "system_settings_set" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/system_settings_set/$KEY%7C$VAL%7C$XTRA;

elif [ "$CNTL" = "gpio_set" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/gpio_set/$KEY%7C$VAL;

elif [ "$CNTL" = "gpio_get" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/gpio_get/$KEY;

elif [ "$CNTL" = "software_update" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.updater/control/software_update;

else

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/$CNTL

fi



