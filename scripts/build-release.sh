#!/usr/bin/env bash
#
# Builds the signed release APK and copies it into release/.
#
# The signing key is created on first run so the script works on a fresh clone.
# Never commit the keystore: it is listed in .gitignore.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
if [[ -z "$ANDROID_HOME" ]]; then
  echo "ANDROID_HOME (or ANDROID_SDK_ROOT) must point at an Android SDK" >&2
  exit 1
fi

KEYSTORE_PATH="${ROULETTE_KEYSTORE:-$ROOT_DIR/keystore/release.jks}"
STORE_PASSWORD="${ROULETTE_STORE_PASSWORD:-roulette2024}"
KEY_ALIAS="${ROULETTE_KEY_ALIAS:-roulette}"
KEY_PASSWORD="${ROULETTE_KEY_PASSWORD:-roulette2024}"

if [[ ! -f "$KEYSTORE_PATH" ]]; then
  echo "No keystore at $KEYSTORE_PATH, generating one"
  mkdir -p "$(dirname "$KEYSTORE_PATH")"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -dname "CN=Roulette Android, OU=Release, O=yuushi2024, L=Tokyo, ST=Tokyo, C=JP"
fi

export ROULETTE_KEYSTORE="$KEYSTORE_PATH"
export ROULETTE_STORE_PASSWORD="$STORE_PASSWORD"
export ROULETTE_KEY_ALIAS="$KEY_ALIAS"
export ROULETTE_KEY_PASSWORD="$KEY_PASSWORD"

./gradlew :app:clean :app:testDebugUnitTest :app:assembleRelease

VERSION_NAME="$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts | head -1)"
APK_NAME="roulette-android-${VERSION_NAME}.apk"

mkdir -p release
cp app/build/outputs/apk/release/app-release.apk "release/$APK_NAME"
(cd release && sha256sum "$APK_NAME" > SHA256SUMS.txt)

echo "Signed APK written to release/$APK_NAME"
