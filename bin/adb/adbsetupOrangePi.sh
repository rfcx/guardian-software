#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

# export RW=$1;
# export KEY=$2;
# export VAL=$3;

export KEYCODE_UP="19"
export KEYCODE_DOWN="20"
export KEYCODE_ENTER="23"
export KEYCODE_BACK="4"
export KEYCODE_HOME="3"

echo "1) System: Setting Default Write Disk to Internal..."
SET_STORAGE=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.android.settings.deviceinfo.Memory && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK && input keyevent $KEYCODE_BACK && input keyevent $KEYCODE_HOME"`
SET_STORAGE=`$ADB shell "pm set-install-location 1"`
GET_STORAGE=`$ADB shell "pm get-install-location"`
echo "   - $GET_STORAGE";

sleep 1;
echo "2) System: Enabling Mobile Data Roaming..."
SET_ROAMING=`$ADB shell "content update --uri content://settings/global/data_roaming --bind value:i:1"`
GET_ROAMING=`$ADB shell "content query --uri content://settings/global/data_roaming"`
echo "   - $GET_ROAMING";

sleep 1;
echo "3) System: Disabling Automatic Timezone Adjustment..."
SET_TIMEZONE=`$ADB shell "content update --uri content://settings/system/auto_time_zone --bind value:i:0"`
GET_TIMEZONE=`$ADB shell "content query --uri content://settings/system/auto_time_zone"`
echo "   - $GET_TIMEZONE";

sleep 1;
echo "4) Installing RFCx Guardian Role..."
INSTALL_GUARDIAN=`$SCRIPT_DIR/../apk/deploy-apk.sh guardian`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`

sleep 1;
echo "5) Installing RFCx Admin Role..."
INSTALL_ADMIN=`$SCRIPT_DIR/../apk/deploy-apk.sh admin`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`

sleep 1;
echo "6) System: Changing Default SMS App to RFCx Admin..."
SET_SMSAPP=`$ADB shell "content update --uri content://settings/secure/sms_default_application --bind value:s:org.rfcx.guardian.admin"`
GET_SMSAPP=`$ADB shell "content query --uri content://settings/secure/sms_default_application"`
echo "   - $GET_SMSAPP";

sleep 1;
echo "7) System: Disabling WiFi Hotspot Timeout..."  
SET_HOTSPOT=`$ADB shell "am start -n com.android.settings/com.android.settings.SubSettings -e :android:show_fragment com.mediatek.wifi.hotspot.TetherWifiSettings && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_UP && input keyevent $KEYCODE_ENTER && input keyevent $KEYCODE_BACK && input keyevent $KEYCODE_BACK && input keyevent $KEYCODE_BACK && input keyevent $KEYCODE_HOME"`
GET_HOTSPOT=`$ADB shell "content query --uri content://settings/system/wifi_hotspot_auto_disable"`
echo "   - $GET_HOTSPOT";

sleep 1;
echo "8) Installing RFCx Updater Role..."
INSTALL_UPDATER=`$SCRIPT_DIR/../apk/deploy-apk.sh updater`
CLOSE_APP=`$ADB shell "input keyevent $KEYCODE_HOME"`




### Still Needed
###
# SD Card Formatting
# Default Storage Disk