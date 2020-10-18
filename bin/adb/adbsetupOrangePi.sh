#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
export PROJECT_DIR="$SCRIPT_DIR/../..";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";
export KEYCODE_UP="19"
export KEYCODE_DOWN="20"
export KEYCODE_ENTER="23"
export KEYCODE_BACK="4"
export KEYCODE_HOME="3"

export BLK='\x1B[0;30m' # Black - Regular
export RED='\x1B[0;31m' # Red
export GRN='\x1B[0;32m' # Green
export YLW='\x1B[0;33m' # Yellow
export BLU='\x1B[0;34m' # Blue
export PUR='\x1B[0;35m' # Purple
export CYN='\x1B[0;36m' # Cyan
export WHI='\x1B[0;37m' # White
export RST='\x1B[0m'    # Text Reset
export CLR=$WHI;

# export ITERATION=$1;
# export SET_GUID=$2;
# export VAL=$3;


echo "";
echo "0) System: Waiting for Connection..."  
for i in {1..50}
do
	echo "   - Connection Attempt #$i";
	ATTEMPT=`$ADB disconnect; sleep 1;`
	ATTEMPT=`$ADB devices;`;
	if [[ $ATTEMPT != *"0123456789ABCDEF"* ]]; then
		sleep 10;
	else
		echo "   - OrangePi is Connected... Please wait a few more moments...";
		sleep 15;
		break
	fi
done
echo "   - Boot is nearly Complete...";
sleep 15;
echo "   - A few more moments...";
sleep 15;
echo "   - Boot Completed... Beginning Setup..."  
sleep 15;



echo "1) System: Formatting SD Card..."  
SET_SDCARD=`$ADB shell "am start -n com.android.settings/com.android.settings.MediaFormat && sleep 2 && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_DOWN && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
sleep 20;
GET_SDCARD=`$ADB shell "df /storage/sdcard0 | grep sdcard"`
echo "   - $GET_SDCARD";

echo "2) Verifying Device IMEI Code..."  
GET_IMEI=`$ADB shell "dumpsys iphonesubinfo" | grep Device`
echo -e "   - IMEI: $GRN${GET_IMEI:14}$RST";

echo "3) System: Rebooting..."  
REBOOT=`$ADB shell "reboot"`

echo "4) Waiting for Reboot to Complete..."  
echo "   - This could take a minute or more, please wait for feedback...";
sleep 10;

for i in {1..50}
do
	echo "   - Waiting for Boot to Complete... (Connection Attempt #$i)";
	ATTEMPT=`$ADB disconnect; sleep 1;`
	ATTEMPT=`$ADB devices;`;
	if [[ $ATTEMPT != *"0123456789ABCDEF"* ]]; then
		sleep 10;
	else
		echo "   - OrangePi is Connected... Please wait a few more moments...";
		sleep 10;
		break
	fi
done
echo "   - Boot is nearly Complete...";
sleep 10;
echo "5) Boot Completed... Continuing Setup..."  
sleep 10;


echo "6) System: Setting Default Write Disk to Internal..."
SET_STORAGE=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.android.settings.deviceinfo.Memory && sleep 2 && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
SET_STORAGE=`$ADB shell "pm set-install-location 1"`
GET_STORAGE=`$ADB shell "pm get-install-location" | grep 'internal' | cut -d'[' -f 1`
if [ "$GET_STORAGE" = "1" ]; then
	echo "   - Success";
else
	echo "   - Failure";
fi

sleep 1;
echo "7) System: Disabling WiFi Hotspot Timeout..."  
SET_HOTSPOT=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.mediatek.wifi.hotspot.TetherWifiSettings && sleep 2 && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK"`
GET_HOTSPOT=`$ADB shell "content query --uri content://settings/system/wifi_hotspot_auto_disable" | grep ' value' | cut -d',' -f 3`
if [[ $GET_HOTSPOT != *"=0" ]]; then
	echo "   - Success";
else
	echo "   - Failure";
fi


sleep 1;
echo "8) System: Enabling Mobile Data Roaming..."
SET_ROAMING=`$ADB shell "content update --uri content://settings/global/data_roaming --bind value:i:1"`
GET_ROAMING=`$ADB shell "content query --uri content://settings/global/data_roaming" | grep ' value' | cut -d',' -f 3`
if [[ $GET_ROAMING != *"=1" ]]; then
	echo "   - Success";
else
	echo "   - Failure";
fi

sleep 1;
echo "9) System: Disabling Automatic Timezone Adjustment..."
SET_TIMEZONE=`$ADB shell "content update --uri content://settings/system/auto_time_zone --bind value:i:0"`
GET_TIMEZONE=`$ADB shell "content query --uri content://settings/system/auto_time_zone" | grep ' value' | cut -d',' -f 3`
if [[ $GET_TIMEZONE != *"=0" ]]; then	
	echo "   - Success";
else
	echo "   - Failure";
fi

sleep 1;
echo "10) Installing RFCx Guardian Role..."
APK_ROLE="guardian"
INSTALL_APK=`$SCRIPT_DIR/../apk/deploy-apk.sh $APK_ROLE nobuild;`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
CHECK_INSTALL=`$ADB shell "cat /data/data/org.rfcx.guardian.$APK_ROLE/files/txt/guid"`
GUID="$CHECK_INSTALL";
echo "   - $CHECK_INSTALL";

sleep 1;
echo "11) Installing RFCx Admin Role..."
APK_ROLE="admin"
INSTALL_APK=`$SCRIPT_DIR/../apk/deploy-apk.sh $APK_ROLE nobuild;`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
CHECK_INSTALL=`$ADB shell "cat /data/data/org.rfcx.guardian.$APK_ROLE/files/txt/guid"`
if [ "$CHECK_INSTALL" = "$GUID" ]; then
	echo "   - Success";
else
	echo "   - Failure";
fi

sleep 1;
echo "12) System: Changing Default SMS App to RFCx Admin..."
SET_SMSAPP=`$ADB shell "content update --uri content://settings/secure/sms_default_application --bind value:s:org.rfcx.guardian.admin"`
GET_SMSAPP=`$ADB shell "content query --uri content://settings/secure/sms_default_application" | grep ' value' | cut -d',' -f 3`
if [[ $GET_SMSAPP != *"=org.rfcx.guardian.admin" ]]; then	
	echo "   - Success";
else
	echo "   - Failure";
fi

sleep 1;
echo "13) Installing RFCx Updater Role..."
APK_ROLE="updater"
INSTALL_APK=`$SCRIPT_DIR/../apk/deploy-apk.sh $APK_ROLE nobuild;`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`
CHECK_INSTALL=`$ADB shell "cat /data/data/org.rfcx.guardian.$APK_ROLE/files/txt/guid"`
if [ "$CHECK_INSTALL" = "$GUID" ]; then
	echo "   - Success";
else
	echo "   - Failure";
fi


FINAL_GUID=`$ADB shell "cat /data/data/org.rfcx.guardian.guardian/files/txt/guid"`
MINI_GUID=`echo "${FINAL_GUID:0:8}" | awk '{print toupper($0)}'`

echo "";
echo "14) Guardian Setup Complete..."  
echo -e "   - GUID: $GRN${MINI_GUID}$RST";


echo "";

