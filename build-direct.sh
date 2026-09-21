#!/usr/bin/env bash
# Compatibility entry point; v0.15 needs Gradle for Kotlin/Room/RecyclerView.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
export PATH="$JAVA_HOME/bin:$PATH"
if [[ ! -f debug.keystore ]]; then base64 --decode debug.keystore.base64 > debug.keystore; fi
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
mkdir -p build/direct
cp app/build/outputs/apk/debug/app-debug.apk build/direct/VGrop-market-v0.15.1-debug.apk
"$ANDROID_HOME/build-tools/35.0.1/apksigner" verify --verbose --print-certs build/direct/VGrop-market-v0.15.1-debug.apk
