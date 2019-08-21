#!/bin/bash

echo "attempting to connect...";

for i in {1..12}
do
    echo "...attempt #$i";
    export ATTEMPT=`adb disconnect; adb connect 192.168.43.1:5555; sleep 1; adb devices;`;
    if [[ $ATTEMPT == *"connected"* ]]
        break
    fi
done

adb logcat -C *:V | grep Rfcx;
