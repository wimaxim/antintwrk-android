#!/usr/bin/env bash
# Copyright 2026 Maxim Novichkov; SPDX-License-Identifier: AGPL-3.0-only
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
if [[ -z "${JAVA_HOME:-}" && -d '/Applications/Android Studio.app/Contents/jbr/Contents/Home' ]]; then
  export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
fi
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21 or newer}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
local_build="$project_root/.antintwrk-local"
mkdir -p "$local_build"
native_keystore="${ANTINTWRK_NATIVE_KEYSTORE:-$local_build/debug.keystore}"
if [[ ! -f "$native_keystore" ]]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$native_keystore" -storepass android -keypass android \
    -alias androiddebugkey -dname 'CN=Local antintwrk development,O=Local,C=US' -keyalg RSA -keysize 2048 -validity 3650
fi
task=':app:assembleGplayDebug'
firebase_resources="$project_root/config/antintwrk-firebase-debug"
if [[ "${1:-}" == '--release' ]]; then
  shift
  task=':app:bundleGplayRelease'
  firebase_resources="$project_root/config/antintwrk-firebase-release"
  : "${ANTINTWRK_UPLOAD_KEYSTORE:?Set a private upload keystore path outside Git}"
  : "${ANTINTWRK_UPLOAD_STORE_PASSWORD:?Set the upload keystore password}"
fi
./gradlew "$task" -PantintwrkOnboarding=true -PantintwrkWebFeed=true -PantintwrkNativeAuth=true \
  "-PantintwrkNativeKeystore=$native_keystore" "-PantintwrkFirebaseResources=$firebase_resources" \
  --console=plain --max-workers=4 "$@"
