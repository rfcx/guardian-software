#!/bin/bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

echo "Assuming that guardian is at 192.168.43.1..."

export GUARDIAN_IP="192.168.43.1"
export PORT="7329"
export IS_CONNECTED="no"

for i in {1..12}
do
	echo "...attempt #$i";
	export ATTEMPT=`$ADB disconnect; sleep 1; $ADB connect $GUARDIAN_IP:$PORT; sleep 1; $ADB devices;`;

	if [[ $ATTEMPT != *"connected"* ]]; then
		echo "trying again..."
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
	echo "RFCx Log Output:"
	echo "-"
	$SCRIPT_DIR/adblogcatRfcx.sh
fi
