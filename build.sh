#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRAMEWORK_RES="${ANDROID_FRAMEWORK_RES:-/system/framework/framework-res.apk}"
ANDROID_JAR="${ANDROID_JAR:-/data/data/com.termux/files/usr/share/java/android-24.jar}"
KEYSTORE="${CREW_TEACHER_KEYSTORE:-$SCRIPT_DIR/test.keystore}"
OUT_APK="$SCRIPT_DIR/CrewTeacher.apk"
VERSION_FILE="$SCRIPT_DIR/version.txt"
if [ -f "$VERSION_FILE" ]; then
    VERSION_NAME="$(tr -d ' \r\n' < "$VERSION_FILE")"
else
    VERSION_NAME="1.1.0"
    echo "$VERSION_NAME" > "$VERSION_FILE"
fi

echo "📦 Target Build Version: v${VERSION_NAME}"

# Update AndroidManifest.xml version strings
VERSION_CODE="$(grep -E "versionCode" app/build.gradle | head -n1 | awk '{print $2}' | tr -d ';\r\n')"
if [ -n "$VERSION_CODE" ]; then
    sed -i -E "s/android:versionCode=\"[^\"]*\"/android:versionCode=\"${VERSION_CODE}\"/g" app/src/main/AndroidManifest.xml
fi
sed -i -E "s/android:versionName=\"[^\"]*\"/android:versionName=\"${VERSION_NAME}\"/g" app/src/main/AndroidManifest.xml

if [ ! -f "$KEYSTORE" ]; then
    echo "Creating development test.keystore..."
    keytool -genkey -v -keystore "$KEYSTORE" -alias crewteacher -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=CrewTeacher, OU=OpenSource, O=CrewPocket, L=Taipei, ST=Taiwan, C=TW" \
        -storepass password -keypass password
fi

cd "$SCRIPT_DIR"
rm -rf bin
mkdir -p bin/classes bin/gen

echo "1. Generating R.java..."
aapt package -f -m -J bin/gen -S app/src/main/res -M app/src/main/AndroidManifest.xml -I "$FRAMEWORK_RES"

echo "2. Compiling Java classes..."
javac -d bin/classes -cp "$ANDROID_JAR:$SCRIPT_DIR/app/libs/*" bin/gen/com/crewpocket/teacher/R.java app/src/main/java/com/crewpocket/teacher/*.java

echo "3. Converting to DEX..."
d8 --output bin/ bin/classes/com/crewpocket/teacher/*.class app/libs/*.jar

echo "4. Packaging APK..."
aapt package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -I "$FRAMEWORK_RES" -F bin/unsigned.apk

echo "4.5 Building Oboe native audio output (arm64-v8a)..."
mkdir -p bin/lib/arm64-v8a bin/oboe-obj
OBOE_SOURCES="$(find third_party/oboe/src -type f -name '*.cpp')"
printf '%s\n' app/src/main/cpp/CrewOboeOutput.cpp $OBOE_SOURCES | xargs -r -n 1 -P 4 sh -c '
  SOURCE="$0"
  OBJECT="bin/oboe-obj/$(basename "${SOURCE%.cpp}").o"
  clang++ -fPIC -std=c++17 -O2 \
    -Ithird_party/oboe/include -Ithird_party/oboe/src -I/data/data/com.termux/files/usr/include \
    -c "$SOURCE" -o "$OBJECT"
'
clang++ -shared -Wl,-z,max-page-size=16384 \
  bin/oboe-obj/*.o -llog -lOpenSLES -o bin/lib/arm64-v8a/libcrewaudio.so
cp /data/data/com.termux/files/usr/lib/libc++_shared.so bin/lib/arm64-v8a/

cd bin
aapt add unsigned.apk classes.dex
aapt add unsigned.apk lib/arm64-v8a/libcrewaudio.so
aapt add unsigned.apk lib/arm64-v8a/libc++_shared.so
cd "$SCRIPT_DIR"

echo "5. Signing APK..."
apksigner sign --ks "$KEYSTORE" \
  --ks-pass "pass:${CREW_TEACHER_KEYSTORE_PASS:-password}" \
  --key-pass "pass:${CREW_TEACHER_KEY_PASS:-password}" \
  --out "$OUT_APK" \
  bin/unsigned.apk

# Copy to external sdcard Download
if [ -d "/sdcard/Download" ]; then
    cp -f "$OUT_APK" "/sdcard/Download/CrewTeacher-v${VERSION_NAME}.apk"
    cp -f "$OUT_APK" "/sdcard/Download/CrewTeacher.apk"
    echo "📦 Copied to /sdcard/Download/CrewTeacher-v${VERSION_NAME}.apk"
fi

# Copy to storage/downloads if available
if [ -d "/data/data/com.termux/files/home/storage/downloads" ]; then
    cp -f "$OUT_APK" "/data/data/com.termux/files/home/storage/downloads/CrewTeacher-v${VERSION_NAME}.apk"
    cp -f "$OUT_APK" "/data/data/com.termux/files/home/storage/downloads/CrewTeacher.apk"
    echo "📦 Copied to /data/data/com.termux/files/home/storage/downloads/CrewTeacher-v${VERSION_NAME}.apk"
fi

echo "✅ SUCCESS: Built CrewTeacher v${VERSION_NAME} ($OUT_APK)"
