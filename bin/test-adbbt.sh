#!/bin/bash

#
# Minimal unit tests for adbbt.sh
#

# TODO(sness) - Consider moving to a framework like bats
# (https://github.com/sstephenson/bats)

# TODO(sness) - Add tests for nth failure

# TODO(sness) - Add test for "adb devices"

fail () {
    echo "Test failed"
    exit 1
}

#
# adbbt.sh Should be able to handle connect
#
echo "adbbt.sh Should be able to handle connect"
export PATH="./test-harness/pass:$PATH"
OUTPUT="$(./adbbt.sh)"
if [[ $OUTPUT != *"Rfcx"* ]]
then
    fail
else
    echo " PASS"
fi

#
# adbbt.sh Should be able to handle failure
#
echo "adbbt.sh Should be able to handle failure"
export PATH="./test-harness/fail:$PATH"
OUTPUT="$(./adbbt.sh)"
if [[ $OUTPUT == *"Rfcx"* ]]
then
    fail
else
    echo " PASS"
fi

#
# All tests passed
#
echo "All tests passed"
