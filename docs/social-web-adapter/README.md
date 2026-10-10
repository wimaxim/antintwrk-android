# Social web adapter source

Operator: Super David Universe LLC. Author: [Maxim Novichkov](https://antintwrk.com/en/hero/2266620).

These exact client-side adapters are AGPL-3.0-only, matching the Android client. They implement the app/web navigation, editor viewport and native Play Billing protocol. They are served by the independent social web product; the in-app authenticated shell is their intended integration surface. No server secrets or private backend are included.

`source-sha256.json` records the deployed source bytes. This is corresponding client adapter source, not a claim that the entire independent remote social web/backend product is AGPL or that bundled native integration can be proprietary. The underlying independently developed services remain separate original works, subject to their own and their dependencies' licences. Root PROJECT-NOTICES and LICENSE apply to these adapters.

Serve app/editor/viewport as ES modules with the editor and viewport next to app.js. The verification/Kindness modules use authenticated same-origin APIs and `window.antintwrkStoreRequest`, never direct Play tokens in JS. Refer to ../../appnav/src/main/kotlin/io/element/android/appnav/antintwrk/AntintwrkNativeWebSignals.kt and AntintwrkStoreCoordinator.kt for the native counterpart. Checkout prices/ownership are not client authority; server receipt verification is required.

The copied sources must be refreshed whenever the deployed adapters change. Public source publication must include the exact adapters used by each distributed build. Signing material, Google publisher credentials and QSNEmain are excluded from the client repository.
