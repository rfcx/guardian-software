export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

version=$1
apps=("guardian" "admin" "updater")

if [ -z $version ]; then
    echo "Usage: download-apk-install.sh VERSION"
	exit 1
fi

ADB_BIN="adb"
if ! type "$ADB_BIN" > /dev/null; then
	# Not found on the path so use ANDROID env var
	ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb.exe"
    echo "Not found 'adb' in PATH, using ANDROID env var(ANDROID_SDK_ROOT)...."
	if ! type "$ADB_BIN" > /dev/null; then
		# Not found ANDROID_SDK_ROOT in ANDROID env var
        ADB_BIN="$ANDROID_SDK_HOME/platform-tools/adb.exe"
        echo "Not found ANDROID_SDK_ROOT, using ANDROID_SDK_HOME...."
        if ! type "$ADB_BIN" > /dev/null; then
            echo "Cannot find 'adb' on your PATH or in ANDROID_SDK_ROOT and ANDROID_SDK_HOME"
            exit 1
        fi
    fi
fi

for app in "${apps[@]}"; do
    #download apk with version from parameter
    curl -O https://s3-eu-west-1.amazonaws.com/rfcx-install/rfcx-guardian/guardian-android-guardian/production/$app-$1.apk

    echo "transferring apk to device...";
    $ADB_BIN push $SCRIPT_DIR/guardian-$1.apk data/local/tmp/$app-$1.apk;

    echo "performing installation...";
    $ADB_BIN shell pm install -f -r data/local/tmp/$app-$1.apk;

    echo "deleting apk...";
    $ADB_BIN shell rm data/local/tmp/$app-$1.apk;

    echo "force relaunch of app role...";
    $ADB_BIN shell am start -n org.rfcx.guardian.$app/.activity.MainActivity;
done
