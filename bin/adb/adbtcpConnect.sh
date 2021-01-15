#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export GUARDIAN_IP="192.168.43.1"
export PORT="7329"
export IS_CONNECTED="no"

echo "-"
echo "Assuming that Guardian is at $GUARDIAN_IP on port $PORT..."

for i in {1..12}
do
	echo "-"
	echo "Connection Attempt #$i...";
	export ATTEMPT=`$ADB disconnect; sleep 1; $ADB connect $GUARDIAN_IP:$PORT; sleep 1; $ADB devices;`;

	if [[ $ATTEMPT != *"connected"* ]]; then
		echo "Trying again..."
		IS_CONNECTED="no"
	else
		echo "-"
		echo "Yes! Successfully connected to ADB!"
		echo "-"
		IS_CONNECTED="yes"
		break
	fi

done

if [ "$IS_CONNECTED" = "yes" ]; then
	echo "RFCx Logcat Feed:"
	echo "-"
	$SCRIPT_DIR/adblogcatRfcx.sh
	export ATTEMPT=`$ADB disconnect; $ADB kill-server; $ADB devices;`;
fi
