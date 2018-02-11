#!/system/bin/sh

CACHE_DIR=/data/data/org.rfcx.guardian.admin/files/logs
LOGCAT_CACHE=$CACHE_DIR/logcat.log
DEST_DIR=/mnt/sdcard/rfcx/log

if [[ ! -d $DEST_DIR ]]
then
  if [[ ! -d /mnt/sdcard/rfcx ]]
  then
    DEST_DIR=$CACHE_DIR
  else
    mkdir $DEST_DIR
    chmod a+rw $DEST_DIR
  fi
fi

if [[ -d $CACHE_DIR ]]
then

  if [[ -f $LOGCAT_CACHE ]]
  then
    chmod a+rw $LOGCAT_CACHE
    TIMESTAMP=$(date +"%s")
    cp $LOGCAT_CACHE $DEST_DIR/$TIMESTAMP.log
    chmod a+rw $DEST_DIR/$TIMESTAMP.log
    rm $LOGCAT_CACHE
  fi

  while true; do

    touch $LOGCAT_CACHE
    logcat -v time > $LOGCAT_CACHE&
    PID=$!

    sleep 1800

    KILL=`kill -9 $PID`
    logcat -c
    chmod a+rw $LOGCAT_CACHE
    TIMESTAMP=$(date +"%s")
    cp $LOGCAT_CACHE $DEST_DIR/$TIMESTAMP.log
    chmod a+rw $DEST_DIR/$TIMESTAMP.log
    rm $LOGCAT_CACHE

  done

fi
