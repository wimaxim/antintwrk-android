# AGENTS.md — Element X Android

## antintwrk UX and account recovery — 7 October 2026

Automate technical friction for users wherever data safety and the agreed trust model permit. The owner approved custodial encryption recovery via the social account. Store the recovery secret in the separate encrypted server vault, never logs/preferences/social DB. Preserve existing identities; never reset a recovery key automatically. Bypass the stock logout warning only after confirming durable escrow, SDK recovery and completed backup upload. A failing vault must not disable working chats/social content. Explain actionable failures in ordinary language. Apply the same product contract to iOS.

## User-Agent соцсети — 7 октября 2026

Для социальных native API и встроенного веба Android/iOS User-Agent обязательно содержит `(+https://qwertynetworks.com)` — URL компании разработчика. Android: `ANTINTWRK Android native-client/1.0 (+https://qwertynetworks.com)`; iOS: `ANTINTWRK iOS native-client/1.0 (+https://qwertynetworks.com)`. Версию обновлять централизованно, URL сохранять. Владелец считает этот маркер достаточным для отнесения User-Agent к относительно доверенным на стороне веба; это классификация клиента, серверная авторизация по сессии сохраняется. Для iOS при подготовке исходников перенести правило в локальный AGENTS.md и настроить одинаковый UA в URLSession/WKWebView. Android использует одну константу для native auth API/WebView; Matrix SDK этим правилом не изменяется.

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
