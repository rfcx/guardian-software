#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export ACTION=$1;

if [ "$ACTION" = "help" ]; then

	echo "";
	echo "This allows you to interact simply with the control content providers of the RFCx roles.";
	echo "Usage Guide:";
	echo "Usage: adbcontrol.sh {[ACTION]}";
	echo "";
	echo "Allowed Actions:";
	echo "reboot kill relaunch screenshot logcat airplanemode_toggle airplanemode_enable sntp_sync";

	echo "";
	exit

elif [ "$ACTION" = "reboot" ]; then
	
	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/reboot
	
elif [ "$ACTION" = "kill" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/kill
	
elif [ "$ACTION" = "relaunch" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/relaunch
	
elif [ "$ACTION" = "screenshot" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/screenshot
	
elif [ "$ACTION" = "logcat" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/logcat
	
elif [ "$ACTION" = "airplanemode_toggle" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/airplanemode_toggle
	
elif [ "$ACTION" = "airplanemode_enable" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/airplanemode_enable
	
elif [ "$ACTION" = "sntp_sync" ]; then

	$ADB shell content query --uri content://org.rfcx.guardian.admin/control/sntp_sync

fi





