#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export AUDIO_DIR=$1;

for AUDIO_FILEPATH in $AUDIO_DIR/*; do

  AUDIO_FILENAME=`echo $AUDIO_FILEPATH | rev | cut -d'.' -f 2 | cut -d'/' -f 1 | rev`;
  AUDIO_EXTENSION=`echo $AUDIO_FILEPATH | rev | cut -d'.' -f 1 | rev`;

  AUDIO_FILE_UNIXTIME=`$SCRIPT_DIR/rename.js --filename $AUDIO_FILENAME`;

  AUDIO_NEW_NAME="$AUDIO_FILE_UNIXTIME.$AUDIO_EXTENSION";

  mv $AUDIO_FILEPATH $AUDIO_DIR/$AUDIO_NEW_NAME;

  echo "$AUDIO_FILENAME - $AUDIO_NEW_NAME";
  
done
