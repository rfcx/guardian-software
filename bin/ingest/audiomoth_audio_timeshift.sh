#!/usr/bin/env bash
PATH="/bin:/sbin:/usr/bin:/usr/sbin:/opt/usr/bin:/opt/usr/sbin:/usr/local/bin:usr/local/sbin:$PATH"

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

# Environmental Customizations
export GNU_DATE_BIN="date"; if [[ "$OSTYPE" == "darwin"* ]]; then GNU_DATE_BIN="gdate"; fi;

DIRECTORY_FILEPATH=$1;
OFFSET_IN_SECONDS=$2

FILE_COUNT=0;
FILE_CONVERT_COUNT=0;

FILE_EXT="flac"

mkdir $DIRECTORY_FILEPATH/_tmp;

for FILEPATH_ORIG in $DIRECTORY_FILEPATH/*; do

	FILENAME_ORIG=$(basename -- "$FILEPATH_ORIG")
	EXTENSION_ORIG=$(echo $FILENAME_ORIG | rev | cut -d'.' -f 1 | rev | tr '[:upper:]' '[:lower:]')
	
	if [ "$EXTENSION_ORIG" = "$FILE_EXT" ]; then 

		if [ -f $FILEPATH_ORIG ] ; then FILE_COUNT=$((FILE_COUNT+1)); fi;

		SITENAME="${FILENAME_ORIG:0:4}"

		YR="${FILENAME_ORIG:5:4}"
		MO="${FILENAME_ORIG:9:2}"
		DY="${FILENAME_ORIG:11:2}"
		HR="${FILENAME_ORIG:14:2}"
		MN="${FILENAME_ORIG:16:2}"
		SC="${FILENAME_ORIG:18:2}"

		OFFSET=$(($OFFSET_IN_SECONDS + 0))

		EPOCH=$(($($GNU_DATE_BIN --date="${YR}-${MO}-${DY} ${HR}:${MN}:${SC} +0000" '+%s')+$OFFSET))

		# FILENAME_MOTH="${SITENAME}_${YR}${MO}${DY}_${HR}${MN}${SC}.flac";

		DATETIME_STR=$($GNU_DATE_BIN --utc --date @${EPOCH} +"%Y%m%d_%H%M%S")

		NEWFILENAME="${SITENAME}_${DATETIME_STR}.flac"
		NEWFILEPATH="${DIRECTORY_FILEPATH}/_tmp/${NEWFILENAME}"

		mv $FILEPATH_ORIG $NEWFILEPATH
		
		if [ -f $NEWFILEPATH ] ; then FILE_CONVERT_COUNT=$((FILE_CONVERT_COUNT+1)); fi;
		echo "${FILE_CONVERT_COUNT}) ${FILENAME_ORIG} - ${NEWFILENAME}"
	fi

done

echo "-----";
echo "COUNT: $FILE_CONVERT_COUNT out of $FILE_COUNT total";

