export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

apps=("guardian" "admin" "updater")

if [ -z "$1" ]; then
    echo "Please provide version number"
else

    for app in "${apps[@]}"; do
        #download apk with version from parameter
        curl -O https://s3-eu-west-1.amazonaws.com/rfcx-install/rfcx-guardian/guardian-android-guardian/production/$app-$1.apk

        export ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb";

        echo "transferring apk to device...";
        $ADB_BIN push $SCRIPT_DIR/guardian-$1.apk data/local/tmp/$app-$1.apk;

        echo "performing installation...";
        $ADB_BIN shell pm install -f -r data/local/tmp/$app-$1.apk;

        echo "deleting apk...";
        $ADB_BIN shell rm data/local/tmp/$app-$1.apk;

        echo "force relaunch of app role...";
        $ADB_BIN shell am start -n org.rfcx.guardian.$app/.activity.MainActivity;

        rm $app-$1.apk
    done

fi