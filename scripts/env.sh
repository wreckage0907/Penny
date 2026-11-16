# Sourced by the scripts and the pre-commit hook, from the repo root.
# Picks JDK 17 (Homebrew) when present, locates the SDK and adb, and defaults
# to the emulator so a connected phone is never touched by accident.

JDK17=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
if [ -d "$JDK17" ]; then
    export JAVA_HOME="$JDK17"
fi

SDK=$(sed -n 's/^sdk.dir=//p' android/local.properties 2>/dev/null)
if [ -z "$SDK" ]; then
    SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
fi
ADB="$SDK/platform-tools/adb"

export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
