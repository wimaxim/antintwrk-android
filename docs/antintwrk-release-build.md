# antintwrk Android build and release inputs

The native client and corresponding adapters are AGPL-3.0-only; project/server rights are in PROJECT-NOTICES. This repo includes client integration source. The independently deployed social backend is not needed to compile it and is not included.

Use Git LFS to obtain tracked upstream assets. JDK 21, Android SDK/platform/build tools matching the version catalog, Android Studio and normal Gradle repositories are required. Kotlin/runtime BOM is 2.4.20. Never build production with the upstream public debug/nightly keys.

From the shared mobile workspace use `bash scripts/build-android.sh --social` for a debug test APK; native flags are antintwrkOnboarding/WebFeed/NativeAuth. The shared build script provisions only paths, not secrets in Git. To build from an independent client checkout, provide the same flags directly to Gradle and your own external signing/resource paths.

For release, create your own Firebase Android registration for `com.antintwrk.app`, supply a generated resource directory containing `values/firebase.xml` with your Firebase project configuration, and an external upload keystore. The antintwrk deployment expects project effective-fire-159508; no sender/Publisher private keys belong in the app. Pass antintwrkFirebaseResources and set ANTINTWRK_UPLOAD_KEYSTORE / ANTINTWRK_UPLOAD_STORE_PASSWORD in the local environment. Alias is antintwrk-upload. Use `:app:bundleGplayRelease`, `-PantintwrkVersionName=1.0.0`, `-PantintwrkVersionCode=20261022` and the native flags. Our shared helper `bash scripts/build-android.sh --social --release` reads ignored local build inputs and refuses debug Firebase fallback. Never put passwords in command-line arguments or logs.

The upload certificate signs the AAB submitted to Play. Google Play App Signing uses its distribution certificate for the installed app. Native Google OAuth, Passkey/Digital Asset Links and push registrations must use the actual distribution certificate/package. Draft upload metadata is not a substitute. Release OAuth callback is com.antintwrk.app:/; debug retains its existing callback. SDK private material remains per social account.

Stock FDroid still builds without Play Billing; it is separate from our Google Play product. Corresponding JS adapters are under social-web-adapter with exact-source hashes and ES-module setup instructions.

No publication or merchant activation is implied by a successful build. The signed AAB is a release candidate for Console/internal-track acceptance, not proof of Google review or a successful real purchase. Shared report ../.. / docs/play-software-preparation-2026-10-09.md records outstanding production/signature/UGC/privacy/deletion/physical-device gates. Local commits only; push on explicit owner command.
