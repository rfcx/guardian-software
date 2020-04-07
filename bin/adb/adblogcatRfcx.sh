#!/bin/bash

export ADB="$ANDROID_SDK_ROOT/platform-tools/adb";

export BLK='\x1B[0;30m' # Black - Regular
export RED='\x1B[0;31m' # Red
export GRN='\x1B[0;32m' # Green
export YLW='\x1B[0;33m' # Yellow
export BLU='\x1B[0;34m' # Blue
export PUR='\x1B[0;35m' # Purple
export CYN='\x1B[0;36m' # Cyan
export WHI='\x1B[0;37m' # White
export RST='\x1B[0m'    # Text Reset
export CLR=$WHI;

while IFS="/" read logstdn; do

	if [ "${logstdn:2:4}" = "Rfcx" ]; then
		PRE_PID="${logstdn%%(*}";
		TAG="${PRE_PID:7}";
		LVL="${PRE_PID:0:1}";
		MSG="${logstdn#*:}";

		if [ "$LVL" = "V" ]; then CLR=$PUR;
		elif [ "$LVL" = "D" ]; then CLR=$CYN;
		elif [ "$LVL" = "I" ]; then CLR=$GRN;
		elif [ "$LVL" = "W" ]; then CLR=$YLW;
		elif [ "$LVL" = "E" ]; then CLR=$RED;
		elif [ "$LVL" = "F" ]; then CLR=$RED;
		fi
		echo -e "$LVL $CLR$TAG:$MSG$RST";
	fi

done < <($ADB logcat -v brief *:V) 







