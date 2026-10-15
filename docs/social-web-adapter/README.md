Mobile gratitude fourth choice is now 500000 points (nominal USD500); web remains1000000. Store prices are ProductDetails only. Historical orders retain their original snapshots. Shared audit: `../../../docs/gratitude-mobile-500-2026-10-15.md`.

# Social web adapter source

Operator: Super David Universe LLC. Author: [Maxim Novichkov](https://antintwrk.com/en/hero/2266620).

These exact client-side adapters are AGPL-3.0-only, matching the Android client. They implement the app/web navigation, editor viewport and native Play Billing protocol. They are served by the independent social web product; the in-app authenticated shell is their intended integration surface. No server secrets or private backend are included.

`source-sha256.json` records the deployed source bytes. This is corresponding client adapter source, not a claim that the entire independent remote social web/backend product is AGPL or that bundled native integration can be proprietary. The underlying independently developed services remain separate original works, subject to their own and their dependencies' licences. Root PROJECT-NOTICES and LICENSE apply to these adapters.

Serve app/editor/viewport as ES modules with the editor and viewport next to app.js. The verification/Kindness modules use authenticated same-origin APIs and `window.antintwrkStoreRequest`, never direct Play tokens in JS. Refer to ../../appnav/src/main/kotlin/io/element/android/appnav/antintwrk/AntintwrkNativeWebSignals.kt and AntintwrkStoreCoordinator.kt for the native counterpart. Checkout prices/ownership are not client authority; server receipt verification is required.

The copied sources must be refreshed whenever the deployed adapters change. Public source publication must include the exact adapters used by each distributed build. Signing material, Google publisher credentials and QSNEmain are excluded from the client repository.

The gratitude-native module implements a paid author gift. Select an exact store product and press the one payment button: the authenticated server resolves the publication/reel author and binds that author to the order before Google checkout. Only a verified, granted receipt credits the author directly. The payer balance is not used, and no separate gift confirmation or send step follows payment.

The shared monetary selector keeps real localized ProductDetails prices on a compact line and shows the recipient, selected price and points. There are no duplicate package buttons or fabricated prices. Pending/cancel/retry/close/reopen states retain a non-secret actor/target-bound request identity; success appears only after the server confirms the author credit. Repeated checkout reconciles an existing order first. Refunds reverse the corresponding author credit. Native capability `gratitudePurchaseVersion: 1` is required; older binaries must update rather than fall back to a purchase for the payer.

Android 1.0.44 (440) adds this binding. iOS needs the equivalent StoreKit/server integration; the shared web presentation alone does not establish iOS billing parity. See `../../../docs/gratitude-paid-author-2026-10-15.md` for the contract and acceptance limits.

15 October2026: after confirmed author credit the native adapter signals the shared web shell to close checkout and show a green success dialog with the existing HeroWorld-style fireworks. Only server-paid results trigger it. See `../../../docs/gratitude-success-2026-10-15.md`; no Android binary update required.
