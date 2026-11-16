# Sourced by the scripts and the pre-commit hook, from the repo root.
# Picks JDK 17 (Homebrew) when present, locates the SDK and adb, and defaults
# to the emulator so a connected phone is never touched by accident.

# JDK 17: Gradle 8.13 cannot run on newer JDKs (the Studio JBR is 25).
# Homebrew arm64, then Intel, then any registered 17 (Temurin/sdkman).
for p in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17; do
    if [ -d "$p/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="$p/libexec/openjdk.jdk/Contents/Home"
        break
    fi
done
if [ -z "$JAVA_HOME" ]; then
    JH="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    [ -n "$JH" ] && export JAVA_HOME="$JH"
fi
if [ -z "$JAVA_HOME" ]; then
    echo "env.sh: no JDK 17 found — brew install openjdk@17" >&2
    exit 1
fi

SDK=$(sed -n 's/^sdk.dir=//p' android/local.properties 2>/dev/null)
if [ -z "$SDK" ]; then
    SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
fi
ADB="$SDK/platform-tools/adb"

export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
