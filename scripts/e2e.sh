#!/bin/sh -e
# Run the instrumented e2e suite on the emulator — the same suite CI runs.
# Boots the "penny" AVD headless if no emulator is online.
cd "$(dirname "$0")/.."
. scripts/env.sh

if ! "$ADB" devices | grep -q "^$ANDROID_SERIAL[[:space:]]*device"; then
    "$SDK/emulator/emulator" -avd penny -port "${ANDROID_SERIAL##*-}" \
        -no-snapshot -no-window \
        -gpu swiftshader_indirect -noaudio -no-boot-anim &
    "$ADB" wait-for-device
    n=0
    until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        n=$((n + 1))
        if [ "$n" -gt 90 ]; then
            echo "e2e.sh: emulator did not boot within 180s" >&2
            exit 1
        fi
        sleep 2
    done
fi

(cd android && ./gradlew connectedDebugAndroidTest "$@")
