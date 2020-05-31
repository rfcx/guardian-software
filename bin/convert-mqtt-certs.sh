#!/usr/bin/env bash

## Converts a server certificate into a BKS keystore for use in Android.
## The BKS keystore will be protected with PASSPHRASE and placed in the
## res/raw directory.
##
## Example: ./convert-mqtt-certs.sh ../../rfcx-api/bin/mosquitto/certs/server.crt l0veTr33s

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
export PROJECT_DIR="$SCRIPT_DIR/..";

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 SERVER_CERT PASSPHRASE"
    exit 1
fi

SERVER_CERT=$1 # ../../rfcx-api/bin/mosquitto/certs/server.crt
PASSPHRASE=$2

keytool -import -alias mqttbroker -file $SERVER_CERT -keystore $PROJECT_DIR/lib-core/src/main/res/raw/rfcx_mqtt_broker.bks  -storetype BKS -storepass $PASSPHRASE -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath $PROJECT_DIR/bin/tools/bcprov-ext-jdk15to18-165.jar
