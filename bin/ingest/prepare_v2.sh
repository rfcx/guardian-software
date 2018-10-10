#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export DATE_CONV_SCRIPT="$SCRIPT_DIR/_iso_to_unix_timestamp.js";
rm -f $DATE_CONV_SCRIPT;
echo "#!/usr/bin/env node" > $DATE_CONV_SCRIPT;
echo "var args = process.argv.slice(2);" >> $DATE_CONV_SCRIPT;
echo "console.log((new Date(Date.parse(args[0]))).valueOf());" >> $DATE_CONV_SCRIPT;
chmod a+x $DATE_CONV_SCRIPT;

export GUARDIAN_GUID=$1;
export AUDIO_DIR=$2;

export GUARDIAN_NAME="label";
export AUDIO_FORMAT="opus";

FILES_PER_DIRECTORY=3990;

FILE_COUNT=0;
DIR_COUNT=0;

if [ ! -d $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME ] ; then mkdir -p $AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME 2> /dev/null; fi;

for AUDIO_DIR_YEAR_MONTH in $AUDIO_DIR/20*; do

  for AUDIO_DIR_DAY in $AUDIO_DIR_YEAR_MONTH/20*; do

    for AUDIO_DIR_HOUR in $AUDIO_DIR_DAY/*; do

      echo $AUDIO_DIR_HOUR;
      
      gunzip $AUDIO_DIR_HOUR/*.$AUDIO_FORMAT.gz 2> /dev/null;

      for AUDIO_FILE in $AUDIO_DIR_HOUR/*.$AUDIO_FORMAT; do

        AUDIO_DIR_TEST=`echo $AUDIO_FILE | rev | cut -d'_' -f 2 | rev`;

        if [ $AUDIO_DIR_TEST != $AUDIO_FILE ] ; then

          TS_DATE_DELIM="T";
          TS_TIME_DELIM=":";
          TS_ZONE_DELIM="-";
          TS_BASE=`echo $AUDIO_FILE | rev | cut -d'_' -f 1 | rev`;
          TS_DATE=`echo $TS_BASE | cut -d'T' -f 1`;
          TS_HOUR=`echo $TS_BASE | cut -d'T' -f 2 | cut -d'.' -f 1 | cut -d'-' -f 1`;
          TS_MIN=`echo $TS_BASE | cut -d'T' -f 2 | cut -d'.' -f 1 | cut -d'-' -f 2`;
          TS_SEC=`echo $TS_BASE | cut -d'T' -f 2 | cut -d'.' -f 1 | cut -d'-' -f 3`;
          TS_MILLISEC=`echo $TS_BASE | cut -d'.' -f 2 | cut -d'-' -f 1`;
          TS_OFFSET=`echo $TS_BASE | cut -d'.' -f 2 | cut -d'-' -f 2`;
          TS_MILLISEC_NUMBER=$((TS_MILLISEC+0));
          TS_MILLISEC_PADDED=`printf %03d $TS_MILLISEC_NUMBER`;

          TS="$TS_DATE$TS_DATE_DELIM$TS_HOUR$TS_TIME_DELIM$TS_MIN$TS_TIME_DELIM$TS_SEC.$TS_MILLISEC_PADDED$TS_ZONE_DELIM$TS_OFFSET";

          if (( $FILE_COUNT % $FILES_PER_DIRECTORY == 0 )) ; then DIR_COUNT=$((DIR_COUNT+1)); fi

          DIR_COUNT_STR=`printf %02d $DIR_COUNT`;

          DEST_DIR="$AUDIO_DIR/$GUARDIAN_GUID-$GUARDIAN_NAME/$GUARDIAN_GUID-$DIR_COUNT_STR";

          if [ ! -d $DEST_DIR ] ; then mkdir -p $DEST_DIR 2> /dev/null; fi;

          export TS_UNIX=`$DATE_CONV_SCRIPT $TS;`;

          mv $AUDIO_FILE $DEST_DIR/$TS_UNIX.$AUDIO_FORMAT;

          if [ -f $DEST_DIR/$TS_UNIX.$AUDIO_FORMAT ] ; then FILE_COUNT=$((FILE_COUNT+1)); fi;

        fi;

      done

    done

  done
  
done

echo "-----";
echo "COUNT: $FILE_COUNT";