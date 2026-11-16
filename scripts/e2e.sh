#!/bin/sh -e
# Run the instrumented e2e suite on the emulator — the same suite CI runs.
# Boots the "penny" AVD headless if no emulator is online.
cd "$(dirname "$0")/.."
. scripts/env.sh

if ! "$ADB" devices | grep -q "^$ANDROID_SERIAL[[:space:]]*device"; then
    "$SDK/emulator/emulator" -avd penny -no-snapshot -no-window \
        -gpu swiftshader_indirect -noaudio -no-boot-anim &
    "$ADB" wait-for-device
    until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        sleep 2
    done
fi

(cd android && ./gradlew connectedDebugAndroidTest "$@")
