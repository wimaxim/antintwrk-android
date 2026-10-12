# antintwrk Android

Android application for the antintwrk social network, with an integrated encrypted messenger and calls. Project operator: **Super David Universe LLC**. Project visionary and founder: **[Maxim Novichkov](https://antintwrk.com/en/hero/2266620)**.

This repository contains the complete modified Android client, including native social integration and the corresponding [web adapters](docs/social-web-adapter/README.md). The client derives from Element X Android and is **AGPL-3.0-only**. Original upstream copyright and dependency notices are preserved. See [PROJECT-NOTICES](PROJECT-NOTICES.md), [LICENSE](LICENSE), and the [original upstream README](README-UPSTREAM.md).

The independently developed social backend and remote web product are separate works and are not included here. Their proprietary notice does not restrict the AGPL rights in this client or its bundled integrations. No commercial Element licence is claimed.

## Build

Install Git LFS, JDK 21+, and the Android SDK (platform/build tools specified in the Gradle version configuration; currently target SDK 37). Clone this repository with LFS enabled, then:

```sh
git lfs pull
bash tools/build-antintwrk.sh -PantintwrkVersionName=1.0.26 -PantintwrkVersionCode=20261050 --no-configuration-cache
```

`JAVA_HOME` and `ANDROID_HOME` can override the local runtime paths. The script creates a local development key in ignored `.antintwrk-local/`; it is not the project's private signing key. Set `ANTINTWRK_NATIVE_KEYSTORE` to an existing private debug keystore to use that signature. The server only admits registered Android signatures to native Google/passkey routes. A locally signed build must have its signature registered for those services; it does not impersonate the official build.

Firebase XML under `config/` contains only public Android client configuration, not a service account or FCM sender key. Configuration is selected independently for `com.antintwrk.app.debug` and `com.antintwrk.app`. `tools/prepare-firebase-android.py` reproduces these resources from the corresponding Firebase Android client download. Private Google publisher credentials and signing keys stay outside this repository.

For a signed release bundle, use `bash tools/build-antintwrk.sh --release` with `ANTINTWRK_UPLOAD_KEYSTORE` and `ANTINTWRK_UPLOAD_STORE_PASSWORD` supplied privately. The upload key alias is `antintwrk-upload`. Version arguments select name/base code; the build adds an ABI digit (arm64=2, universal bundle=0). Builds are written under `app/build/outputs/`.

## Checks

```sh
./gradlew :appnav:ktlintCheck :appnav:testDebugUnitTest --tests '*Antintwrk*' --no-configuration-cache
node tools/tests/incoming-share.mjs
```

The original FDroid build remains available with `./gradlew :app:assembleFdroidDebug`; it does not enable the antintwrk native shell or Play Billing. See [upstream build instructions](README-UPSTREAM.md#build-instructions) for the underlying project.

## Current integration

Social content selects the Russian or global origin using server-side IP geography. The version in shared app settings appends **R** for the applied Russian social origin and **G** for the global origin. Messenger connectivity remains independent. Android system Share opens a publication draft in the social network; text and supported gallery media are reviewed before the user publishes them. Existing drafts, account boundaries and server publication validation are preserved.

Support is available inside the social network. This is a modified independent application, not an official Element distribution.
