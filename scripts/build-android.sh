#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────
#  Kinetix — Build Android APK
#  Run from the repository root: scripts/build-android.sh
# ──────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android-controller"

echo ""
echo "============================================"
echo "  Kinetix — Android APK Build"
echo "============================================"
echo ""

# Check for Gradle wrapper
if [ ! -f "$ANDROID_DIR/gradlew" ]; then
    echo "[!] gradlew not found. Creating wrapper..."
    cd "$ANDROID_DIR"
    gradle wrapper --gradle-version 8.5 2>/dev/null || {
        echo "[!] 'gradle' command not found."
        echo "    Open the project in Android Studio and let it generate the wrapper,"
        echo "    or install Gradle and re-run this script."
        exit 1
    }
fi

cd "$ANDROID_DIR"
chmod +x gradlew

echo "[1/2] Cleaning previous build..."
./gradlew clean

echo ""
echo "[2/2] Building debug APK..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo ""
    echo "============================================"
    echo "  ✅  Build successful!"
    echo "  APK: $ANDROID_DIR/$APK_PATH"
    echo "============================================"
else
    echo ""
    echo "  ❌  APK not found at expected path."
    exit 1
fi
