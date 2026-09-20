#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
export PATH="$JAVA_HOME/bin:$PATH"
bt="$ANDROID_HOME/build-tools/35.0.1"
jar="$ANDROID_HOME/platforms/android-35/android.jar"
out="build/direct"
rm -rf "$out/classes" "$out/dex" "$out/generated"
mkdir -p "$out/classes" "$out/dex" "$out/generated"
"$bt/aapt2" compile --dir app/src/main/res -o "$out/resources.zip"
"$bt/aapt2" link -o "$out/unsigned.apk" -I "$jar" --manifest app/src/main/AndroidManifest.xml --min-sdk-version 31 --target-sdk-version 35 --java "$out/generated" "$out/resources.zip"
find app/src/main/java "$out/generated" -name '*.java' > "$out/sources.txt"
javac -source 17 -target 17 -classpath "$jar" -d "$out/classes" @"$out/sources.txt"
find "$out/classes" -name '*.class' > "$out/classes.txt"
"$bt/d8" --lib "$jar" --min-api 31 --output "$out/dex" @"$out/classes.txt"
python3 - "$out" <<'PY'
import sys,zipfile,pathlib
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'unsigned.apk','a') as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$bt/zipalign" -f 4 "$out/unsigned.apk" "$out/aligned.apk"
if [[ ! -f debug.keystore && -f debug.keystore.base64 ]]; then
 base64 --decode debug.keystore.base64 > debug.keystore
fi
if [[ ! -f debug.keystore ]]; then
 keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
fi
"$bt/apksigner" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out "$out/VGrop-market-v0.14.0-debug.apk" "$out/aligned.apk"
"$bt/apksigner" verify --verbose "$out/VGrop-market-v0.14.0-debug.apk"
