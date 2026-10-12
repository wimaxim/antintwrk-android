# AGENTS.md — Element X Android

## antintwrk UX and account recovery — 7 October 2026

Automate technical friction for users wherever data safety and the agreed trust model permit. The owner approved custodial encryption recovery via the social account. Store the recovery secret in the separate encrypted server vault, never logs/preferences/social DB. Preserve existing identities; never reset a recovery key automatically. Bypass the stock logout warning only after confirming durable escrow, SDK recovery and completed backup upload. A failing vault must not disable working chats/social content. Explain actionable failures in ordinary language. Apply the same product contract to iOS.

## User-Agent соцсети — 7 октября 2026

Для социальных native API и встроенного веба Android/iOS User-Agent обязательно содержит `(+https://qwertynetworks.com)` — URL компании разработчика. Android: `ANTINTWRK Android native-client/1.0 app/<versionName> (+https://qwertynetworks.com)`; iOS: `ANTINTWRK iOS native-client/1.0 app/<CFBundleShortVersionString> (+https://qwertynetworks.com)`. `native-client/1.0` — версия протокола; app/version — версия установленного приложения из общего источника сборки, без R/G. Старые приложения без app/version сохраняют совместимость. Версию обновлять централизованно, URL сохранять. Владелец считает этот маркер достаточным для отнесения User-Agent к относительно доверенным на стороне веба; это классификация клиента, серверная авторизация по сессии сохраняется. Для iOS при подготовке исходников перенести правило в локальный AGENTS.md и настроить одинаковый UA в URLSession/WKWebView. Android использует одну константу для native auth API/WebView; Matrix SDK этим правилом не изменяется.

> **Repo:** `element-hq/element-x-android` — Android Matrix client (Compose UI + `matrix-rust-sdk`).

---

## antintwrk development rules

After completed changes, run appropriate checks, update documentation, and create a local commit before reporting completion. This includes functional milestones, fixes, rules, and documentation. Ordinary local commits are authorized on an ongoing basis.

Push only upon an explicit command from the owner to send commits. This replaces the earlier automatic-push authorization as of 6 October 2026. A configured private `origin`, a completed milestone, restored connectivity, or an earlier failed push is not authorization to push. Do not automatically send pending commits or resume prior push retries. When explicitly instructed to push, verify that the remote branch contains the sent commit.

Keep secrets, user data, build artifacts, logs, and unrelated changes out of commits. Never push to the official Element `upstream` or `QSNEmain`. If an explicitly requested push fails, resolve it using an existing authorized method while that command remains in effect, including GitHub Desktop; report unconfirmed publication honestly. Do not remove workflows or disable required checks to bypass failures. Do not change visibility, rewrite history, or force-push without a separate instruction.

Rules and substantial documentation changes follow the same process. See [antintwrk milestone workflow](docs/antintwrk-development.md) for the shared Android/iOS procedure.

## Strong Conventions

PRs must meet these rules.

### Code Style

- Style enforced by **Editor config** (`.editorconfig`).
- Set "Hard wrap at" to 160 chars in Android Studio.

### PII & Logging

- We use **Timber** for logging. Never use `android.util.Log`.
- **Never log secrets, passwords, keys, or user content** (e.g. message bodies).
- Matrix IDs (User IDs, Room IDs, Event IDs) are safe to log.

### Strings & Localisation

- Default localisation: `en` (en-GB strings), shared with Element X iOS via [Localazy](https://localazy.com/p/element).
- **Never edit `localazy.xml`** — it is auto-generated and overwritten.
- New English strings go in **`temporary.xml`**. The core team imports these to Localazy.
- **Key naming**:
  - Cross-screen verbs: `action_` (e.g., `action_copy`).
  - Common nouns/other: `common_` (e.g., `common_error`).
  - Accessibility: `a11y_`.
  - Screen-specific: `screen_<name>_<key>` (e.g., `screen_onboarding_welcome_title`).
  - Errors: `error_` prefix.
  - Platform-specific: `_ios` or `_android` suffix.
  - Placeholders: Use numbered form `%1$s`, `%2$d`.

### Previews

- Create previews for **all main states** of a Composable.
- Use `@PreviewsDayNight` for consistency.
- Use `PreviewParameterProvider` (e.g., `FooStatePreviewParam`) to provide states.
- Wrap previews in `ElementPreview { ... }`.
- When writing tests, never try to record the screenshots, the CI will do it.

---

## Pull Request Guidelines

- Sentence-style titles (no conventional commits).
- Exactly one `pr-` label (see `.github/release.yml`).
- Title = changelog entry — descriptive, no "Fixes #…".
- Leave description template for the developer. Redirect them to the [contributing etiquette](CONTRIBUTING.md#etiquette).
- Screenshots/videos for visual changes.
- 500 additions max into production code. Test code can be larger — split large changes.
- Commits need a title and description; no tiny or massive commits.
- No history rewrites.
- When creating a pull request and if changes contain new or update(s) of the Composable Previews, add the label "Record-Screenshots" to the PR so that the CI will record the screenshots.

---

## Project Structure

### Build System

Common Gradle tasks:
- Build: `./gradlew assembleDebug`
- Unit Tests: `./gradlew test`
- Lint: `./gradlew lint`
- Format: `./gradlew ktlintFormat`
- Update Docs TOC: `./gradlew generateDocsToc`

### Gradle Modules

Features follow a 3-module structure:
- `features/foo/api`: Public interfaces and data classes.
- `features/foo/impl`: Internal implementation, Presenter, and View.
- `features/foo/test`: Test fakes and utilities.

---

## Architecture: Appyx + Molecule

We use [Appyx](https://bumble-tech.github.io/appyx/) for navigation and [Molecule](https://github.com/cashapp/molecule) for Presenters.

### Files Per Screen (`Foo`)

| File | Purpose |
| :--- | :--- |
| `FooNode.kt` | Appyx Node: Handles navigation and wires the Presenter to the View. |
| `FooPresenter.kt` | A `@Composable` function that produces `FooState` from `FooEvent`s. |
| `FooView.kt` | Stateless Composable rendering the UI from `FooState`. |
| `FooState.kt` | Data class representing the immutable UI state. |
| `FooEvent.kt` | Sealed interface for UI actions sent to the Presenter. |
| `FooStatePreviewParam.kt` | Provides sample states for Previews and Screenshot tests. |
| `FooPresenterTest.kt` | Unit tests for the Presenter logic using Turbine. |

---

## Dependency Injection (Metro)

- We use [Metro](https://zacsweers.github.io/metro/) for DI.
- Inject via constructor parameters using `@Inject`.
- Use `@AssistedInject` and `@AssistedFactory` for components requiring runtime arguments (like Navigators or IDs).
- Use `@ContributesBinding(AppScope::class)` for singleton-like services.
- Use `@ContributesNode(RoomScope::class)` for Appyx Nodes.

---

## Compound Design System

Always prefer Compound components and tokens from `libraries/compound/` module.

- **Colours**: `ElementTheme.colors.textPrimary`, `ElementTheme.colors.bgCanvasDefault`.
- **Typography**: `ElementTheme.typography.fontBodyMdRegular`.
- **Icons**: Use `CompoundIcons.IconName()` (e.g., `CompoundIcons.UserProfileSolid()`).

---

## The Rust SDK Layer

We wrap the `matrix-rust-sdk` to isolate the UI from the underlying SDK.
- Naming: SDK `Room` → `JoinedRoom` or `RoomInfo`.
- Type Mapping: Map Rust SDK types to Kotlin data classes in the `api` module to avoid leaking `MatrixRustSDK` into the UI.
- Always follow Kotlin naming conventions (e.g., `userId` instead of `userID`).

## Тестовые APK — актуальная сборка

По поручению владельца от 8 октября 2026: после подтверждённой загрузки нового APK в папку Google Drive «antintwrk — тестовые сборки» перемещать прежние APK из этой папки в корзину Drive. Оставлять один актуальный APK; не трогать другие файлы или папки. Не удалять рабочий старый файл до успешной загрузки нового. Это постоянное разрешение на обычное удаление в корзину; не очищать корзину без отдельного поручения.

## Mandatory Android → iOS adaptation record

Before modifying the managed shell or messenger, read ../docs/mobile-adaptation-index.md and the shared common/decisions/Android/iOS documents. Every completed adaptation must update the shared contract, Android implementation/validation, iOS parity requirements and this index in the same task. Record source anchors, actual tests and remaining limitations; do not keep decisions only in chat or silently treat historical reports as current. This rule must be carried into the future iOS AGENTS.md.

## Public corresponding Android source — 10 October 2026

The owner explicitly instructed making wimaxim/antintwrk-android public so the in-app Source Code link is accessible. Publish the corresponding current client source and standalone build instructions for this task. This scoped instruction overrides development-only private visibility for Android, and does not authorize any iOS/QSNEmain publication, history rewrite, or upstream push. Subsequent routine pushes still require the owner's explicit command. Do not commit private signing/service credentials or backend/backup material.

## Shared media and invitations — 10 October 2026

Each explicitly accepted system Share starts a fresh actor-bound UUID composer; do not reject it merely because an editor exists. Preserve ordinary feed/profile drafts and wait for an active file chooser/native prompt to finish. Never publish automatically. Capture results may only use the exact staged camera URI and matching actor/epoch/page; arbitrary private providers remain forbidden. Inviting friends belongs only in common Settings, outside DM details. Select one contact then open a localized SMS draft; fall back to the system share chooser if contact/SMS handling is unavailable. Use public antintwrk.com only. Group member invitation retains messenger search; Profile opens the social resolver. Keep technical room details hidden in managed debug builds too. Document every change in the shared Android/iOS adaptation index.

## Managed personal-chat deletion and own version — 10 October 2026

Display own app versions without the test marker. Managed 1:1 Delete chat only hides the list entry on this device/account; never call leave/forget/delete or alter history, membership, keys or receipts. Persist visibility in the private session directory, restore automatically on a new received message (including still encrypted messages) and on an explicit room route. Filter before pagination and exclude hidden old unread counts; keep background room sync and new-message notifications. Groups retain membership invitation/leave. Explain visibility and automatic return in a confirmation. Original GNU LICENSE and upstream LICENSE-COMMERCIAL stay intact; own rights belong in PROJECT-NOTICES. Document Android/iOS parity and validate failure/cancellation/account isolation.


## Unified app invitations — 11 October 2026

The owner's new instruction supersedes the contact picker/SMS-only invitation rule above. All invitations to install/join the app (common Settings, Chats plus menu and the messenger InviteFriendsUseCase) open the standard system text sharesheet immediately. Use the same localized screen_ant_invite_message and public https://antintwrk.com; no Matrix permalink/IDs/transport domain, forced target app, contacts lookup or automatic sending. Recipient and final send remain user-controlled. Existing-group membership invitation is a separate action. Port this contract with UIActivityViewController on iOS and document both entry paths.


## Managed group creation and DM settings — 11 October 2026

New room from Chats plus is a group: select at least two distinct users other than the actor before creating anything on the server. No Skip; collect selections first, include invitations in createRoom and guard both UI and presenter. Spaces themselves retain their folder workflow. Personal chats are created through Social profile routes or messenger user search. Hide People in managed DM settings from authoritative isDm even before the other member is loaded. Explicit operator test-history resets preserve account identities/recovery and must never become automatic error recovery; document server versus device cleanup.


## DM actions and incoming-call permission — 11 October 2026

Managed personal chats hide Invite/Share/People using direct metadata even before profile loading; groups retain membership invitation/sharing. The Chats new-room action and group wizard use localized New group chat, preserving minimum two other invitees/no Skip. Do not infer DM from member count alone or reset messages/keys to correct m.direct. Full-screen incoming-call access on Android14+ is checked at app foreground entry independently of account preparation: request only when absent, do not auto-grant, do not loop on own Settings return/rotation, and never block login on refusal/platform errors. Keep corresponding iOS/backend contract in shared docs/direct-actions-call-permission-2026-10-11.md.


## Personal-chat actions and reciprocal reply — 11 October 2026

Own SDK uses Matrix-spec direct metadata, never two-member-count alone to identify DM. Settings and long-press menus replace Leave with local Delete in personal chats before member details load, with event-level leave guards. Explicit social/search intent to write accepts a matching incoming personal invitation and opens its original ID; never auto-accept on startup or fall back to another create after join/sync failure. Both routes share validation and actor-bound create/recheck serialization. Keep iOS contract and actual validation in shared docs/personal-chat-reply-2026-10-11.md. Do not reset rooms/identities/keys as recovery.


## Versioned social requests — 11 October 2026

Keep the actual installed product version in the centralized social User-Agent for native HTTP and every embedded WebView, on both social origins. Preserve native-client/1.0 and (+https://qwertynetworks.com); never put R/G, identities or secrets in it. Build/version metadata must share one source. Explicit operator room resets preserve account identities and recovery, never become automatic client recovery. See shared docs/client-version-and-reset-2026-10-11.md and Google Play launch plan for verified release boundaries.


## Logout isolation and startup animation — 11 October 2026

Logout must remove every Domain/Path/HttpOnly variant in the app-private embedded cookie jar and await completion before new admission; never assume expiring a host-only cookie of the same name deletes a Domain cookie. Clear retained pages and identity hints, invalidate admission results/finally, preserve actor matching for late SDK logout, and use only old snapshots for best-effort revocation. A persisted logged-out installation must not revive old cookies on the next explicit login. Preserve histories/identities/recovery and external browser cookies. Startup animation lasts at least2 seconds of visible foreground frames while root initialization continues underneath; save elapsed time across recreation, do not repeat on warm resume, and defer permission screens until visible interval ends. Port this contract to iOS with its separate WKWebView/URLSession stores. See shared docs/logout-startup-2026-10-11.md.


## Pending message keys — 11 October 2026

Managed timeline waiting for keys shows localized inline indeterminate progress with an explanation. Remove it through SDK content updates without room refresh; keep readable history/composer available, and preserve explicit withheld/trust errors. Historical pending keys after join use the approved managed recovery contract, not a blanket denial. UI must not reset identities/history or bypass authorization. Record Android/iOS parity in shared docs/message-keys-loading-2026-10-11.md.


## Signed-device key sharing — 11 October 2026

Managed SDK clients always use IDENTITY_BASED_STRATEGY regardless of the hidden isolation feature flag. Unsigned devices must not block signed devices and must not receive room keys through an automatic trust override. Preserve current-device verification, changed-identity checks and actor-bound custody restore. Never auto-ignore trust, sign arbitrary other devices, reset keys/history or switch to AllDevices to suppress a warning. iOS must carry the same contract; see shared docs/signed-device-sharing-2026-10-11.md.


## Fresh-session sending readiness — 11 October 2026

Clearing app data on the same phone creates a fresh crypto session and is an ordinary supported scenario. Freeze managed SDK send queues before restore/UI exposure. Resume only with actual SDK Verified and Running sync, observing initial readiness without waiting for a failed send; pause immediately on loss and cancel delayed enables with the session scope. Keep mounted chat content/drafts and show a small localized preparing/retry row; never fake Verified, reset identity/history, or require manual trust overrides. A vault retry must not disable an already verified working device. See shared docs/fresh-login-send-gate-2026-10-11.md and port the same queue gate to iOS.


## Account-recovery informational shields — 11 October 2026

For managed same-realm social actors/senders without explicit third-party forwarding, omit only the noncritical AuthenticityNotGuaranteed notice from ordinary bubble/dialog/action-summary UI. This is presentation under the approved custodial recovery model, not proof of original device authenticity or a fake Verified. Preserve raw SDK provenance/strict inspection and all critical, mismatch, identity, device and cleartext warnings. Refresh UI shield metadata without resetting history or recreating the chat. Same policy is required for iOS; see shared docs/account-recovery-notice-2026-10-11.md.
