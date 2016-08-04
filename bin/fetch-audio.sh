#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export DESTINATION_DIR="/Users/topher/Desktop";
export ORIGIN_DIR="2016-07/25-AM";
export GUID=$1;

adb pull /sdcard/rfcx/audio/$ORIGIN_DIR/$GUID.opus.gz $DESTINATION_DIR/.;
open $DESTINATION_DIR/$GUID.opus.gz;
sleep 3;
ffmpeg -i $DESTINATION_DIR/$GUID.opus -flags +bitexact -ar 8000 $DESTINATION_DIR/$GUID.wav;
ffmpeg -i $DESTINATION_DIR/$GUID.opus -loglevel panic -nostdin -ac 1 -ar 8000 sox - | sox -t sox - -n spectrogram -h -r -o  $DESTINATION_DIR/$GUID.png -x 2048 -y 512 -w Dolph -z 95 -s -d 90;
rm $DESTINATION_DIR/$GUID.opus.gz $DESTINATION_DIR/$GUID.opus;

echo $GUID;
