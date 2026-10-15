Mobile gratitude fourth choice is now 500000 points (nominal USD500); web remains1000000. Store prices are ProductDetails only. Preserve reconciliation of pre-existing million-point pending gifts. Shared audit: `../../../docs/gratitude-mobile-500-2026-10-15.md`.

# Social web adapter source

Operator: Super David Universe LLC. Author: [Maxim Novichkov](https://antintwrk.com/en/hero/2266620).

These exact client-side adapters are AGPL-3.0-only, matching the Android client. They implement the app/web navigation, editor viewport and native Play Billing protocol. They are served by the independent social web product; the in-app authenticated shell is their intended integration surface. No server secrets or private backend are included.

`source-sha256.json` records the deployed source bytes. This is corresponding client adapter source, not a claim that the entire independent remote social web/backend product is AGPL or that bundled native integration can be proprietary. The underlying independently developed services remain separate original works, subject to their own and their dependencies' licences. Root PROJECT-NOTICES and LICENSE apply to these adapters.

Serve app/editor/viewport as ES modules with the editor and viewport next to app.js. The verification/Kindness modules use authenticated same-origin APIs and `window.antintwrkStoreRequest`, never direct Play tokens in JS. Refer to ../../appnav/src/main/kotlin/io/element/android/appnav/antintwrk/AntintwrkNativeWebSignals.kt and AntintwrkStoreCoordinator.kt for the native counterpart. Checkout prices/ownership are not client authority; server receipt verification is required.

The copied sources must be refreshed whenever the deployed adapters change. Public source publication must include the exact adapters used by each distributed build. Signing material, Google publisher credentials and QSNEmain are excluded from the client repository.

The gratitude-native module keeps Kindness selection, current Google prices and a single context-sensitive gift/purchase action inside the gratitude dialog. Purchases replenish only the sender; sending a gift requires a separate explicit confirmation. Interrupted gifts retain an actor/recipient-bound retry identity; no Google token or authoritative balance is stored by the adapter. The existing independent web module supplies the modal/CSRF context. Native binaries are unchanged by this source mirror.

The selected amount controls the sole main action. A sufficient balance leads to explicit gift review; otherwise only an exact matching store product can be purchased, using its real localized price. No repeated product buttons, automatic gift on purchase, fabricated price, or automatic multi-package purchase is allowed.

The gratitude entry dialog now uses the same monetary amount fieldset and styling as the ordinary web page. Store-formatted prices remain visible regardless of balance; there is no separate points-only selector or balance/recipient header. Review and idempotent gift handling remain a separate confirmation state. Unavailable store products are not assigned fabricated prices.

15 October2026: keep the complete localized store price on one compact line. Gift review shows selected package price and points, explicitly stating no additional payment for a gift deducted from the existing balance. Missing prices are never fabricated.
