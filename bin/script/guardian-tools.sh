#!/bin/zsh
set -e

function menu() {
    clear
    echo "======== List of function ======="
    echo "1. Remove registration"
    echo "2. Downgrade version (need APKs file)"
    echo "3. Remove classifier"
    echo "-other to exit-"
    echo "================================="
    echo "Enter above number"
    read userChoice

    if [ "$userChoice" = "1" ]; then
        removeRegistration
    elif [ "$userChoice" = "2" ]; then
        downgradeSoftware
    elif [ "$userChoice" = "3" ]; then
        removeAllClassifiers
    else
        exitGuardian
    fi
}

function chooseToMenu() {
    echo "Do you want to continue ? (y/n)"
    read toMenu

    if [ "$toMenu" = "y" ]; then
        menu
    else
        exitGuardian
    fi
}

function connectGuardian() {
    echo "Connect to the Guardian over wifi"

    read "?Press Enter to continue"

    echo "Connecting..."
    adb connect 192.168.43.1:7329
}

function exitGuardian() {
    clear
    echo "Disconnecting..."
    adb disconnect 192.168.43.1:7329

    echo "Done!"
    read "?Press Enter to exit"
}

function removeRegistration() {
    clear
    echo "Removing registration..."
    adb shell "rm /data/data/org.rfcx.guardian.guardian/files/txt/keystore_passphrase"
    adb shell "rm /data/data/org.rfcx.guardian.guardian/files/txt/pin_code"
    adb shell "rm /data/data/org.rfcx.guardian.guardian/files/txt/token"
    adb shell "am force-stop org.rfcx.guardian.guardian"
    adb shell "monkey -p org.rfcx.guardian.guardian 1"
    echo "Done!"
    chooseToMenu
}

function listAndChooseSoftware() {
    clear
    files=($(ls -d $1*.apk))
    fileslength=${#files[@]}

    echo
    for (( i=1; i<=${fileslength}; i++ )); do
        echo "${i}. ${files[$i]}"
    done

    echo "Enter above number"
    read fileChoice

    echo
    echo "Uninstalling $1..."
    adb uninstall org.rfcx.guardian.$1
    echo "Installing ${files[$fileChoice]}..."
    adb install ${files[$fileChoice]}
    adb shell "monkey -p org.rfcx.guardian.$1 1"

    echo
    echo "Done!"
    menu
}

function downgradeSoftware() {
    clear
    echo "======== List of software ======="
    echo "1. guardian"
    echo "2. admin"
    echo "3. classify"
    echo "4. updater"
    echo "-other to return to menu-"
    echo "================================="
    echo "Enter above number"
    read downgradeChoice

    if [ "$downgradeChoice" = "1" ]; then
        listAndChooseSoftware "guardian"
    elif [ "$downgradeChoice" = "2" ]; then
        listAndChooseSoftware "admin"
    elif [ "$downgradeChoice" = "3" ]; then
        listAndChooseSoftware "classify"
    elif [ "$downgradeChoice" = "4" ]; then
        listAndChooseSoftware "updater"
    else
        menu
    fi
}

function removeAllClassifiers() {
    clear
    echo "Removing classifiers..."
    adb shell "sqlite3 /data/data/org.rfcx.guardian.guardian/databases/library-classifier.db \"DELETE FROM classifier;\""
    adb shell "sqlite3 /data/data/org.rfcx.guardian.guardian/databases/audio-classifier-active.db \"DELETE FROM active;\""
    
    echo "Done!"
    chooseToMenu
}

connectGuardian
menu
