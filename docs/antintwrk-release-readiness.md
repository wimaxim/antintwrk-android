# Release preparation — 12 October 2026

Managed version1.0.26 adds a direct Delete account entry to shared account settings. It opens the admitted social deletion request page on the applied social origin. A deletion request uses private support intake and operator ownership verification; it does not silently deactivate the account or erase another person's data. Independent public request information is available at https://antintwrk.com/antintwrk-account-deletion.html without app installation or account cookies.

The native gplay source set has its own manifest overlay removing the upstream REQUEST_INSTALL_PACKAGES permission. ANTINTWRK uses Play distribution updates and does not offer installing other applications as a product feature. Attachments remain downloadable and shareable. The upstream and FDroid manifest remains unchanged.

206 managed integration tests in40 suites and app/appnav formatting passed. Version1.0.26 debug/release builds use the existing development/upload signatures and own Firebase configuration. Bundle/signature/manifest and16KiB ELF/ZIP alignment checks are recorded in shared release evidence; these checks do not replace a physical16KB device test.

Real Play checkout is not activated. Console products require a Billing-enabled build in a test track. Upload/review remains subject to the owner's separate instruction. After internal upload, use the actual Play distribution certificate for OAuth/DAL/passkey associations, then verify Play-installed login, calls, push, encrypted recovery and licensed purchase/refund scenarios. A local APK does not prove those production-signature flows.

App content/Data safety must describe the social WebView and managed messenger together, including operator-assisted key recovery, voluntary identity review, IP geography, social content, device/push IDs and purchases. Do not claim operator-inaccessible messages or automatic deletion merely because a request button exists. Private backend, keys and user data stay outside this repository.
