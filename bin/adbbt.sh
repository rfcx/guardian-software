#!/bin/bash

echo "attempting to connect...";

echo "...attempt #1";
export ATTEMPT_1=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
if [[ $ATTEMPT_1 != *"connected"* ]]
then
  echo "...attempt #2";
  export ATTEMPT_2=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
  if [[ $ATTEMPT_2 != *"connected"* ]]
  then
    echo "...attempt #3";
    export ATTEMPT_3=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
    if [[ $ATTEMPT_3 != *"connected"* ]]
    then
      echo "...attempt #4";
      export ATTEMPT_4=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
      if [[ $ATTEMPT_4 != *"connected"* ]]
      then
        echo "...attempt #5";
        export ATTEMPT_5=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
        if [[ $ATTEMPT_5 != *"connected"* ]]
        then
          echo "...attempt #6";
          export ATTEMPT_6=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
          if [[ $ATTEMPT_6 != *"connected"* ]]
          then
            echo "...attempt #7";
            export ATTEMPT_7=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
            if [[ $ATTEMPT_7 != *"connected"* ]]
            then
              echo "...attempt #8";
              export ATTEMPT_8=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
              if [[ $ATTEMPT_8 != *"connected"* ]]
              then
                echo "...attempt #9";
                export ATTEMPT_9=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
                if [[ $ATTEMPT_9 != *"connected"* ]]
                then
                  echo "...attempt #10";
                  export ATTEMPT_10=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
                  if [[ $ATTEMPT_10 != *"connected"* ]]
                  then
                    echo "...attempt #11";
                    export ATTEMPT_11=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
                    if [[ $ATTEMPT_11 != *"connected"* ]]
                    then
                      echo "...attempt #12";
                      export ATTEMPT_12=`adb disconnect; adb connect 192.168.43.1; sleep 1; adb devices;`;
                      if [[ $ATTEMPT_12 != *"connected"* ]]
                      then
                        echo "failed after 12 attempts...";
                      fi
                    fi
                  fi
                fi
              fi
            fi
          fi
        fi
      fi
    fi
  fi
fi

adb logcat -C *:V | grep Rfcx;
