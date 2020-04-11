#!/usr/bin/env bash

## Converts a server certificate into a BKS keystore for use in Android.
## The BKS keystore will be protected with PASSPHRASE and placed in the
## res/raw directory.
##
## Example: ./convert-mqtt-certs.sh ../../rfcx-api/bin/mosquitto/certs/server.crt l0veTr33s

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 SERVER_CERT PASSPHRASE"
    exit 1
fi

SERVER_CERT=$1 # ../../rfcx-api/bin/mosquitto/certs/server.crt
PASSPHRASE=$2

keytool -import -alias mqttbroker -file $SERVER_CERT -keystore ../core/src/main/res/raw/server.bks  -storetype BKS -storepass $PASSPHRASE -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-ext-jdk15to18-165.jar
