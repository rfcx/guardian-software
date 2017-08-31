#!/usr/bin/env bash

## Script Execution Syntax:
## ./ingest.sh [API_USER_GUID] [API_USER_TOKEN] [AUDIO_FORMAT] [GUARDIAN_GUID] [AUDIO_DIRECTORY_PATH]
##

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export DATE_CONV_SCRIPT="$SCRIPT_DIR/date_conv.js";
rm -f $DATE_CONV_SCRIPT;
echo "#!/usr/bin/env node" > $DATE_CONV_SCRIPT;
echo "var args = process.argv.slice(2);" >> $DATE_CONV_SCRIPT;
echo "var TIMESTAMP = ((args[args.indexOf(\"--timestamp\")] === (\"--timestamp\")) ? args[args.indexOf(\"--timestamp\")+1] : \"\");" >> $DATE_CONV_SCRIPT;
echo "var GUARDIAN = ((args[args.indexOf(\"--guardian\")] === (\"--guardian\")) ? args[args.indexOf(\"--guardian\")+1] : \"\");" >> $DATE_CONV_SCRIPT;
echo "var dateString = (new Date(parseInt(TIMESTAMP))).toISOString().substr(0,19).replace(/:/g,\"-\");" >> $DATE_CONV_SCRIPT;
echo "var s3Path = \"/\"+dateString.substr(0,4)+\"/\"+dateString.substr(5,2)+\"/\"+dateString.substr(8,2)+\"/\"+GUARDIAN+\"/\"+GUARDIAN+\"-\"+dateString;" >> $DATE_CONV_SCRIPT;
echo "console.log(s3Path+'*'+(new Date(parseInt(TIMESTAMP))).toISOString()+'*');" >> $DATE_CONV_SCRIPT;
chmod a+x $DATE_CONV_SCRIPT;

export API_USER=$1;
export API_TOKEN=$2;

export AUDIO_FORMAT=$3;

export GUARDIAN_GUID=$4;
export AUDIO_DIR=$5;

if [[ "$AUDIO_FORMAT" == "m4a" ]]; then
  AUDIO_FORMAT_ID="4";
  AUDIO_FORMAT_SAMPLE_RATE="8000";
else
  AUDIO_FORMAT_ID="3";
  AUDIO_FORMAT_SAMPLE_RATE="12000";
fi

export PRE_PREP=`rm -rf $AUDIO_DIR/*.wav; mkdir $AUDIO_DIR/-complete;`;

for AUDIO_FILEPATH in $AUDIO_DIR/*.$AUDIO_FORMAT; do

  export AUDIO_FILENAME=`echo $AUDIO_FILEPATH | rev | cut -d'.' -f 2 | cut -d'/' -f 1 | rev`;
  export AUDIO_EXTENSION=`echo $AUDIO_FILEPATH | rev | cut -d'.' -f 1 | rev`;
  export AUDIO_SHA1=`openssl dgst -sha1 $AUDIO_FILEPATH | grep 'SHA1(' | cut -d'=' -f 2 | cut -d' ' -f 2`;
  export AUDIO_FILESIZE=`ls -l $AUDIO_FILEPATH | cut -d' ' -f 5`;  # -f 8 for OSX, -f 5 for linux

  export AUDIO_CONVERT=`ffmpeg -loglevel panic -i $AUDIO_FILEPATH -ar $AUDIO_FORMAT_SAMPLE_RATE $AUDIO_DIR/$AUDIO_FILENAME.wav`;
  export AUDIO_SAMPLE_COUNT=`soxi -s $AUDIO_DIR/$AUDIO_FILENAME.wav`;
  export AUDIO_WAV_DELETE=`rm -rf $AUDIO_DIR/$AUDIO_FILENAME.wav;`;

  export AUDIO_DATETIME_VALS=`$DATE_CONV_SCRIPT --timestamp $AUDIO_FILENAME --guardian $GUARDIAN_GUID;`;
  export AUDIO_S3_PATH=`echo $AUDIO_DATETIME_VALS | cut -d'*' -f 1`;
  export AUDIO_MEASURED_AT=`echo $AUDIO_DATETIME_VALS | cut -d'*' -f 2`;
  export AUDIO_S3_URL="s3://rfcx-ark/audio$AUDIO_S3_PATH.$AUDIO_EXTENSION";
  
  export AUDIO_S3_SAVE=`aws s3 cp $AUDIO_FILEPATH $AUDIO_S3_URL --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers;`; 

  export CURL_JSON="{ \"format_id\": $AUDIO_FORMAT_ID, \"measured_at\" : \"$AUDIO_MEASURED_AT\", \"size\": $AUDIO_FILESIZE, \"sha1_checksum\": \"$AUDIO_SHA1\", \"capture_sample_count\": $AUDIO_SAMPLE_COUNT }";
  curl -X POST -H "x-auth-user: user/$API_USER" -H "x-auth-token: $API_TOKEN" -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d "$CURL_JSON" "https://api.rfcx.org/v1/guardians/$GUARDIAN_GUID/audio";

  export AUDIO_FILE_CLEANUP=`mv $AUDIO_FILEPATH $AUDIO_DIR/-complete/.`;
  echo "$AUDIO_S3_URL — $AUDIO_SHA1 — $AUDIO_FILESIZE — $AUDIO_SAMPLE_COUNT";


done