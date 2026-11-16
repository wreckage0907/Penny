#!/bin/sh -e
# Build, unit-test, install and launch the native app on the emulator.
# The "run it" step of the migration loop.
cd "$(dirname "$0")/.."
. scripts/env.sh

(cd android && ./gradlew test assembleDebug)
"$ADB" install -r android/app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n com.penny.app/.MainActivity
