#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";
export KEYCODE_UP="19"
export KEYCODE_DOWN="20"
export KEYCODE_ENTER="23"
export KEYCODE_BACK="4"
export KEYCODE_HOME="3"


export ITERATION=$1;
# export SET_GUID=$2;
# export VAL=$3;


echo "";


if [ "$ITERATION" = "1" ]; then


	echo "1) System: Formatting SD Card..."  
	SET_SDCARD=`$ADB shell "am start -n com.android.settings/com.android.settings.MediaFormat && sleep 2 && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
	sleep 10;
	GET_SDCARD=`$ADB shell "df /storage/sdcard0 | grep sdcard"`
	echo "   - $GET_SDCARD";
	echo "2) System: Rebooting..."  
	REBOOT=`$ADB shell "reboot"`


elif [ "$ITERATION" = "2" ]; then


	echo "1) System: Setting Default Write Disk to Internal..."
	SET_STORAGE=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.android.settings.deviceinfo.Memory && sleep 2 && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
	SET_STORAGE=`$ADB shell "pm set-install-location 1"`
	GET_STORAGE=`$ADB shell "pm get-install-location"`
	echo "   - $GET_STORAGE";

	sleep 1;
	echo "2) System: Disabling WiFi Hotspot Timeout..."  
	SET_HOTSPOT=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.mediatek.wifi.hotspot.TetherWifiSettings && sleep 2 && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
	GET_HOTSPOT=`$ADB shell "content query --uri content://settings/system/wifi_hotspot_auto_disable"`
	echo "   - $GET_HOTSPOT";

	sleep 1;
	echo "3) System: Enabling Mobile Data Roaming..."
	SET_ROAMING=`$ADB shell "content update --uri content://settings/global/data_roaming --bind value:i:1"`
	GET_ROAMING=`$ADB shell "content query --uri content://settings/global/data_roaming"`
	echo "   - $GET_ROAMING";

	sleep 1;
	echo "4) System: Disabling Automatic Timezone Adjustment..."
	SET_TIMEZONE=`$ADB shell "content update --uri content://settings/system/auto_time_zone --bind value:i:0"`
	GET_TIMEZONE=`$ADB shell "content query --uri content://settings/system/auto_time_zone"`
	echo "   - $GET_TIMEZONE";

	sleep 1;
	echo "5) Installing RFCx Guardian Role..."
	INSTALL_GUARDIAN=`$SCRIPT_DIR/../apk/deploy-apk.sh guardian`
	CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
	CHECK_GUARDIAN=`$ADB shell "ls /data/data/org.rfcx.guardian.guardian/files/txt/guid"`
	echo "   - $CHECK_GUARDIAN";

	sleep 1;
	echo "6) Installing RFCx Admin Role..."
	INSTALL_ADMIN=`$SCRIPT_DIR/../apk/deploy-apk.sh admin`
	CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
	CHECK_ADMIN=`$ADB shell "ls /data/data/org.rfcx.guardian.admin/files/txt/guid"`
	echo "   - $CHECK_ADMIN";

	sleep 1;
	echo "7) System: Changing Default SMS App to RFCx Admin..."
	SET_SMSAPP=`$ADB shell "content update --uri content://settings/secure/sms_default_application --bind value:s:org.rfcx.guardian.admin"`
	GET_SMSAPP=`$ADB shell "content query --uri content://settings/secure/sms_default_application"`
	echo "   - $GET_SMSAPP";

	sleep 1;
	echo "8) Installing RFCx Updater Role..."
	INSTALL_UPDATER=`$SCRIPT_DIR/../apk/deploy-apk.sh updater`
	CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
	CHECK_UPDATER=`$ADB shell "ls /data/data/org.rfcx.guardian.updater/files/txt/guid"`
	echo "   - $CHECK_UPDATER";


	FINAL_GUID=`$ADB shell "cat /data/data/org.rfcx.guardian.guardian/files/txt/guid"`
	echo "";
	echo "   - guid: $FINAL_GUID";

fi

echo "";

