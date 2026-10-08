# Android test build — 8 October 2026

Own native build targets the Russian app-only backend. Logical IDs are `@uID:antintwrk.com`; HTTPS transport remains `matrix/auth/rtc/turn/push.antimsg.com`, social origin `antimsg.com`. Exact full actor comparison prevents mixed-realm session reuse. Old test realm remains a private operator rollback, not a migration.

Temporary OAuth clients no longer initialize cross-signing or backups. The final session client initializes signing once; account recovery then creates secret storage/backup after verification and durable social-account escrow. All three encrypted signing secrets must exist under the active SSSS key before restore is accepted. Existing backups are never deleted by recovery. Concurrent recovery attempts serialize per full actor. Native FTUE shows automatic preparation or retry instead of manual Element key/reset screens; notification consent remains separate. Native signed-in screens use dark theme. Upstream flag-off behavior remains available.

Native version properties: `antintwrkVersionCode=20261008`, `antintwrkVersionName=0.1.0-test.20261008.1`; ARM64 APK versionCode is 202610082 (ABI suffix). Own debug signing/FCM resources stay outside Git.

Validated: own gplay build and affected ktlint checks; 96 appnav tests (native integration and root navigation), 8 FTUE tests; flag-off fdroid build and FTUE tests. Emulator API 36: fresh native login, automatic setup, encrypted message exchange, complete uninstall/reinstall, same-account restore and reading both previous messages without a manual key; dark chat UI. Credentials remain private. UI/native keyboard automation required careful focus; server login was separately checked through the Russian ingress.

This is a test build. Physical-device/provider quality, Play release signing and iOS are not accepted by these checks. Shared report records server cutover, call validation and the delivered APK fingerprint.
