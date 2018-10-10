#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export GUARDIAN_GUID=$1;
export AUDIO_DIR=$2;

export GUARDIAN_NAME="label";
export AUDIO_FORMAT="opus";

FILES_PER_DIRECTORY=3990;

FILE_COUNT=0;
DIR_COUNT=0;

if [ ! -d $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME ] ; then mkdir -p $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME 2> /dev/null; fi;

for AUDIO_DIR_YEAR_MONTH in $AUDIO_DIR/20*; do

  for AUDIO_DIR_DAY_TIME in $AUDIO_DIR_YEAR_MONTH/*M; do

    echo $AUDIO_DIR_DAY_TIME;

    gunzip $AUDIO_DIR_DAY_TIME/*.$AUDIO_FORMAT.gz 2> /dev/null;

    for AUDIO_FILE in $AUDIO_DIR_DAY_TIME/*.$AUDIO_FORMAT; do

      AUDIO_FILENAME=`echo $AUDIO_FILE | rev | cut -d'.' -f 2 | cut -d'/' -f 1 | rev`;
      AUDIO_EXTENSION=`echo $AUDIO_FILE | rev | cut -d'.' -f 1 | rev`;

      if (( $FILE_COUNT % $FILES_PER_DIRECTORY == 0 )) ; then DIR_COUNT=$((DIR_COUNT+1)); fi

      DIR_COUNT_STR=`printf %02d $DIR_COUNT`;

      DEST_DIR="$AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME/$GUARDIAN_GUID-$DIR_COUNT_STR";

      if [ ! -d $DEST_DIR ] ; then mkdir -p $DEST_DIR 2> /dev/null; fi;

      mv $AUDIO_FILE $DEST_DIR/.;

      if [ -f $DEST_DIR/$AUDIO_FILENAME.$AUDIO_EXTENSION ] ; then FILE_COUNT=$((FILE_COUNT+1)); fi;

    done

  done
  
done

echo "-----";
echo "COUNT: $FILE_COUNT";