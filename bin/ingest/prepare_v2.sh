#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export GUARDIAN_GUID=$1;
export AUDIO_DIR=$2;

export GUARDIAN_NAME="label";
export AUDIO_FORMAT="opus";

FILES_PER_DIRECTORY=3990;

FILE_COUNT=0;
DIR_COUNT=0;

TS_DATE_DELIM="T";
TS_TIME_DELIM=":";
DATETIME_ISO_FORMAT="%Y-%m-%dT%H-%M-%S%z"

if [ ! -d $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME ] ; then mkdir -p $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME 2> /dev/null; fi;

for AUDIO_DIR_YEAR_MONTH in $AUDIO_DIR/20*; do

  for AUDIO_DIR_DAY in $AUDIO_DIR_YEAR_MONTH/20*; do

    for AUDIO_DIR_HOUR in $AUDIO_DIR_DAY/*; do

      echo $AUDIO_DIR_HOUR;
      
      gunzip $AUDIO_DIR_HOUR/*.$AUDIO_FORMAT.gz 2> /dev/null;

      for AUDIO_FILEPATH in $AUDIO_DIR_HOUR/*.$AUDIO_FORMAT; do

        AUDIO_DIR_TEST=`echo $AUDIO_FILEPATH | rev | cut -d'_' -f 2 | rev`;

        if [ $AUDIO_DIR_TEST != $AUDIO_FILEPATH ] ; then

          AUDIO_FILENAME=$(basename -- "$AUDIO_FILEPATH")

          if (( $((${#AUDIO_FILENAME}+0)) > 45 )); then

            TS_BASE=`echo $AUDIO_FILENAME | rev | cut -d'_' -f 1 | rev`;
            TS_DATE=`echo $TS_BASE | cut -d'T' -f 1`;
            TS_HOUR_MIN_SEC=`echo $TS_BASE | cut -d'T' -f 2 | cut -d'.' -f 1`;

            TS_MS_AND_ZONE=`echo $TS_BASE | cut -d'.' -f 2 | cut -d'-' -f 1`;
            TS_MS_TXT="${TS_MS_AND_ZONE:0:3}"
            if [ "$TS_MS_TXT" = "000" ]; then
              TS_MS_TXT="0";
            elif [ "${TS_MS_TXT:0:2}" = "00" ]; then
              TS_MS_TXT="${TS_MS_TXT:2:1}"
            elif [ "${TS_MS_TXT:0:1}" = "0" ]; then
              TS_MS_TXT="${TS_MS_TXT:1:2}"
            fi
            TS_MS=$((TS_MS_TXT+0));

            read -r DATETIME_ISO DATETIME_EPOCH_ <<< "$(date -jf "$DATETIME_ISO_FORMAT" '+%Y-%m-%dT%H:%M:%S.000%z %s' "$TS_DATE$TS_DATE_DELIM$TS_HOUR_MIN_SEC${TS_MS_AND_ZONE:3:5}")"

            TS_UNIX=$((DATETIME_EPOCH_*1000+TS_MS))

            if (( $FILE_COUNT % $FILES_PER_DIRECTORY == 0 )) ; then DIR_COUNT=$((DIR_COUNT+1)); fi

            DIR_COUNT_STR=`printf %02d $DIR_COUNT`;

            DEST_DIR="$AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME/$GUARDIAN_GUID-$DIR_COUNT_STR";

            if [ ! -d $DEST_DIR ] ; then mkdir -p $DEST_DIR 2> /dev/null; fi;

            cp $AUDIO_FILEPATH $DEST_DIR/$TS_UNIX.$AUDIO_FORMAT;

            if [ -f $DEST_DIR/$TS_UNIX.$AUDIO_FORMAT ] ; then FILE_COUNT=$((FILE_COUNT+1)); rm $AUDIO_FILE; fi;

          else

            echo "Filename could not be parsed: $AUDIO_FILENAME"

          fi

        fi

      done

    done

  done
  
done

echo "-----";
echo "COUNT: $FILE_COUNT";