#!/usr/bin/env bash

export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

export ACTION_NAME=$1;

ls -l $SCRIPT_DIR/../ | grep "rfcx-guardian-role-" | cut -d':' -f 2 | cut -d'-' -f 4 | while read ROLE; do $SCRIPT_DIR/$ACTION_NAME-apk.sh $ROLE; done

