# Сохранение этапов разработки antintwrk

## Active bottom tab returns to its root — 7 October 2026

Re-selecting the active native bottom tab now returns from an auxiliary web page to its section root; selecting a different tab retains its page, and tapping an existing root scrolls to the top. This fixes the real emulator case where tapping Feed left People open. 49 social tests pass. Background notification tapping opened Notifications after admission; disabling this device removed its sole subscription while account-wide enabled stayed on. Shared Android/iOS documentation updated.


## Recovery validation and device opt-in follow-up — 7 October 2026

Recovery now validates email locally and clears obsolete request errors; all nine locales have an email-only message. The confirmation address/spam hint and prefilled login were verified on the emulator, and the recovery email arrived without changing the password. Firebase opt-in succeeded after the thread fix: one server subscription and local binding, then one designated test device notification in the background with FCM200. Wrong-password rejection followed by correct login succeeded. 48 social tests and42 Firebase tests pass. Notification routing and additional media flows are still being audited.


## Email confirmation and real-account QA — 7 October 2026

Both registration and password-reset confirmation screens show the recipient address and advise checking Spam and marking the message as Not spam. All nine locales are included; the email carries into login while the password is cleared. Registration from the emulator, email activation, native password login, matching web profile, logout/relogin and restart persistence succeeded for the owner-designated test account. Search, communities, empty notifications and the composer were opened without publishing. Firebase social registration now runs on Dispatchers.IO because SDK FID resolution rejects the UI thread; device opt-in is being rechecked. The server distinguishes expired sessions, throttling and unavailable requests without weakening CSRF or budgets. 47 social and42 Firebase unit tests pass; PHP7.3/7.4 guard checks pass. Google/Passkey success and background delivery remain unverified. No push to GitHub.


## Real-account audit started — 7 October 2026

The owner explicitly requested registering a designated test mailbox in the app and checking registration/email activation, sign-in/sign-out/repeat sign-in and internal social screens. This overrides the earlier prohibition on creating QA accounts for this designated flow only. The mailbox is available in the owner's Chrome tab; no registration or credential submission has been performed yet.

Static audit confirmed that native login dispatched incomplete credentials and used a potentially expired remembered CSRF. The presenter now validates email/nonempty password without imposing new-account password rules on existing accounts. The native node suppresses invalid requests and fetches a fresh protected session before each password submission. A dedicated field message is translated into all9 social locales; existing network/server messages are not duplicated.45 social tests pass with no failures/errors/skipped, all9 locale keysets match, and own Firebase gplayDebug builds. These checks do not establish a successful real-account flow. Interactive emulator access remains pending: the computer-use tool does not recognize the standalone emulator app; Android Studio's native file-dialog input is unreliable. The owner has been asked whether ADB UI control is allowed for this audit. No security or attempt limits were disabled. No push.

## Нативная регистрация и восстановление — 7 октября 2026

Email register/reset подключены к защищённому native adapter прежнего register.php. Нативные формы больше не являются предпросмотром в native-сборке; есть сообщения о следующем шаге в письме и возврат ко входу. Google newcomer проходит отдельный профиль/18+/согласие, затем одноразовый cookie-bound ticket допускается прежним writer. Реальный email/Google account не создавался для QA. После Google-входа/регистрации клиент снова проверяет admission; 2FA сохраняется. Новые строки на девяти языках. Проверены35 Android-тестов, PHP lint, профиль +9 отказных границ и16 живых отрицательных HTTP-проверок. File picker зарегистрирован выше сменяющихся auth-экранов, чтобы выбор не терялся при проверке после фона. Полный этап соцсети и итоговая приёмка ещё продолжаются; push не выполнялся.

## Социальная навигация Android — 7 октября 2026

Подключены реальные communities/profile/notifications/discover и остальные разрешённые социальные маршруты в авторизованном WebView. Нативная вкладка синхронизируется с URL и историей WebView; вспомогательные экраны и чужие профили сохраняют раздел входа. `/go`, `/goto` и локализованные варианты открываются внешне с исходными параметрами. Меню «Ещё» возвращает доступ к созданию публикаций, людям, существующим веб-сообщениям, монетизации, рефералам и справочным страницам. Добавлен системный выбор одного/нескольких файлов; отмена и уничтожение WebView завершают callback. Целевые Android-тесты прошли с native-флагом. Этап соцсети продолжается: регистрация, восстановление, сохранение вкладок и медиасценарии ещё проверяются. Это не отметка окончательной готовности. Локальный функциональный commit `fc4e9f6905`; push не выполнялся.

## User-Agent соцсети — 7 октября 2026

Для социальных native API и встроенного веба Android/iOS User-Agent обязательно содержит `(+https://qwertynetworks.com)` — URL компании разработчика. Android: `ANTINTWRK Android native-client/1.0 (+https://qwertynetworks.com)`; iOS: `ANTINTWRK iOS native-client/1.0 (+https://qwertynetworks.com)`. Версию обновлять централизованно, URL сохранять. Владелец считает этот маркер достаточным для отнесения User-Agent к относительно доверенным на стороне веба; это классификация клиента, серверная авторизация по сессии сохраняется. Для iOS при подготовке исходников перенести правило в локальный AGENTS.md и настроить одинаковый UA в URLSession/WKWebView. Android использует одну константу для native auth API/WebView; Matrix SDK этим правилом не изменяется.

## Nightly reports — 7 октября 2026

Запуск GitHub Actions `37614611749` остановился на checkout приватного репозитория с `repository not found`/exit128, до анализа зависимостей. Workflow наследует `permissions: {}`. Для задачи `dependency-analysis` добавлено только `contents: read`; право записи не выдаётся, расписание и проверки сохранены. Kover/Sonar остаётся ограничен официальным репозиторием Element. Проверены синтаксис YAML, область разрешения и diff. Реальный повторный запуск ожидает явно разрешённого push; до него ночные письма могут продолжаться.

Правило владельца от 6 октября 2026 года. Общие правила также находятся в родительском `antintwrk-mobile/AGENTS.md`.

## Коммиты после изменений; push только по команде

Актуальное поручение владельца от 6 октября 2026 года заменяет прежнее правило автоматического push.

После завершённых изменений выполнить соответствующие проверки, обновить документацию и сделать локальный commit до отчёта о завершении. Это относится к функциональным этапам, исправлениям, правилам и документации. Повторное разрешение на обычный локальный commit не требуется; каждый затронутый репозиторий коммитится отдельно.

Push выполнять только по явной команде владельца на отправку. Наличие приватного `origin`, завершение этапа, создание коммита, восстановление связи с GitHub и прежнее разрешение на автоматический push не являются командой на отправку. Ранее неотправленные коммиты также не отправлять самостоятельно. Не запускать автоматические повторные попытки прежней отправки после изменения этого правила.

Порядок завершения:

1. Выполнить проверки, соответствующие изменению, и обновить затронутую документацию. Отделять выполненные проверки от запланированных.
2. Проверить diff и состав коммита: только изменения задачи, без секретов, пользовательских данных, APK, кешей, логов и чужих изменений.
3. Сделать осмысленный commit с заголовком и описанием результата и проверок.
4. В отчёте указать результат, проверки и локальный commit. Явно различать локальное сохранение и отправку на GitHub.

При явной команде на push отправить согласованные коммиты в рабочую ветку собственного приватного `origin` и подтвердить совпадение удалённого SHA с отправленным commit. Не отправлять в официальный Element `upstream` или `QSNEmain`. При ошибке разрешённой отправки разобраться и повторить её доступным авторизованным способом, если владелец не отозвал команду. Не удалять workflow и не отключать обязательные проверки ради обхода ошибки; не менять видимость, не переписывать историю и не применять force-push без отдельного поручения.

Правило действует для общей документации, Android и iOS. При подготовке iOS перенести его в локальный `AGENTS.md` платформы. Общая документация пока имеет только локальную историю, без собственного remote.

## Social web integration checkpoint — 7 October 2026

Android retains three social WebViews, page state and history across native tabs. Web links select the matching native section and Back restores the previous section. The admitted server session supplies the own public profile identity; settings are separate. The native notification badge reuses the existing unread-count authority. The exact-origin, active-main-frame bridge supports navigation, authentication handoff, change signals, system sharing/copy and HTML document export without exposing cookies or session tokens to JavaScript.

Android also handles HTML file selection, JavaScript dialogs, full-screen video, gesture-created popup links and renderer recovery. Document writes run off the UI thread. Logout destroys every retained social page. External links and /go,/goto open separately; trusted HTTPS redirects can open the system browser. Matrix session, encryption and call code remain unchanged.

Validation: 37 social Android unit tests and formatting, app JS contracts, and PHP lint of three changed server modules passed. Owner acceptance remains: Android14+/Chrome WebView, RU/EN, existing real account; scroll feed, switch communities/profile and return; follow a community link and Back; search/notifications at top; select/cancel files; share/copy links; save tax HTML; full-screen video/Back; external and /go,/goto links; offline/retry; logout hides all account content. Real account login, export and visual acceptance have not been claimed. FCM is a separate stage. Local code commit:9ae6f52cea. No push.

## Own Firebase transport checkpoint — 7 October 2026

Owner approved Firebase in My Project effective-fire-159508, Blaze, Android development app com.antintwrk.app.debug, and the antintwrk-fcm-sender service account with roles/firebasecloudmessaging.admin. Firebase added without Analytics; Android app registered. Server JSON credential installed at /etc/antintwrk/firebase-fcm.json, root:www-data0640, parent0750; never copied into web root or Git. Own client resources are outside Git in artifacts/firebase/android-development; native gplay builds require -PantintwrkFirebaseResources pointing there and use our project instead of upstream Element values. Native FCM auto-init is disabled until explicit registration.

A narrow SocialPushReceiver hook consumes only antintwrk.social.v1; Matrix payloads continue through existing handlers. FID registration uses current Firebase register/onRegistered APIs. Device binding and generation reject stale registrations after logout; data-only notifications contain no account identity or message text. Tapping a social notification requests the native notification route, still behind admission. fdroid exposes unsupported social FCM without changing existing UnifiedPush behavior. UI permission/settings and node registration wiring are the next stage; receiving a real background notification is not yet tested.

Validation:40 social unit tests, Firebase Kotlin compilation and formatting passed. Server native/Web Push protocol tests and PHP lint passed. OAuth exchange passed; validation-only request for a deliberately nonexistent FID returned NOT_FOUND/UNREGISTERED (no notification delivered). This verifies API permission, not end-to-end delivery. No push to GitHub.

## Native push settings and gplay APK — 7 October 2026

Android native More menu now opens device push settings. Explicit device opt-in requests Android notification permission, registers the current FID and saves shared category preferences. Device off removes only that device; shared categories clearly apply to all devices. Denied OS permissions expose Android settings; network failures provide Retry. Permission launcher remains composed through foreground admission checks. Admitted foreground sessions refresh FID; rotation rebinds the same account, logout invalidates binding immediately and attempts bounded server unsubscribe. Notification taps open the web notification section after admission; pending taps are consumed once. Browser-only Push panel is hidden only in app mode because device controls are native.

Full :app:assembleGplayDebug and40 social tests pass with our Firebase resources. All9 native social locale keysets match. Fixed gplay resource source-set timing and multi-provider DI selection; normal Matrix handlers and flags-off behavior are kept. APK and credentials remain ignored local artifacts. Real-account opt-in, closed-app FCM delivery and permission/tap UX still require owner/device acceptance. Native own Firebase will need the project's own Matrix push gateway when messenger infrastructure is connected; official Matrix gateway configuration is not repurposed as a social sender. No push.

## Passkey enrollment in app profile — 7 October 2026

Android enables WebView WebAuthentication support FOR_APP only in native-auth builds when the platform feature is supported. App-only Android fetch for passkey create/options uses the native adapter; list/remove retain their existing protected web endpoint. Native creation reuses qsn_passkey_action after full admission and applies the same APK hash allowlist as native login; password, one-time challenge/session/account, UV/RP and credential checks remain authoritative. Normal web origin checks are unchanged. iOS keeps its ordinary HTTPS WebAuthn route until its implementation is prepared. Extra social routes kindness/support-project/ukraine/how-it-works/sms-consent are admitted to embedded presentation, without changing their server/payment logic.

Validation:40 Android social tests and full own-Firebase gplay APK build pass; app-only JS rewrite contract, server PHP lint and browser/native origin/signature/counter tests pass. Live guest negative checks deny all four push actions and both enrollment actions with401 after valid CSRF bootstrap. Real Passkey enrollment still requires the owner: Android14+/current WebView, RU/EN, own account Profile settings -> security -> add key, password -> system prompt -> success; cancel/retry and wrong-password paths must leave no key. No test account or actual key was created. No push.

## Завершение реализации соцчасти Android — 7 октября 2026

Код нативного входа/регистрации, защищённых веб-разделов и навигации, системных действий, push и оформления подготовлен в gplayDebug. Это тестовая сборка; реальная авторизованная/фоновая приёмка не завершена. Чек-лист: ../docs/android-social-acceptance.md в общем мобильном проекте. Matrix-авторизация/шифрование/звонки не заменялись; интеграция его инфраструктуры отдельно.

Нативные launcher/splash/имя/9 locale settings условны: без флагов исходный Element сохраняет свои значения. Лицензии доступны до входа и в меню: исходный AGPL-текст, Element X/авторы и417 зависимостей (включая non-SPDX). Private FCM JSON только на сервере; временная локальная копия удалена после сравнения hash. Public Firebase config/resources и подпись игнорируются Git. Повторная сборка: bash scripts/build-android.sh --social из родительского проекта; prepare-firebase-android.py проверяет проект/пакет перед подготовкой XML. Gplay с native-флагом без своей конфигурации не использует upstream Firebase. FCM auto-init активируется после успешной явной регистрации устройства; отключение соцподписки не выключает SDK мессенджера.

Дополнительные границы: социальный FCM callback и Matrix installation update выполняются независимо; медленный social API не задерживает Matrix. Counter requests не чаще10 секунд; серверный budget status относится к допущенному user_id, чтобы участники за одним NAT не делили его. Engine savecookie сохраняет host-only/Secure/HttpOnly/Lax при обновлении native социальной сессии; встроенный веб также обходит IP/UA cached identity и проверяет настоящий cookie. Маркер UA сам не даёт авторизацию. Обычный браузер/другие проекты сохраняются.

Проверки:40 social tests +41 Firebase tests без skipped/failures; собственный gplay APK, исходный fdroid без наших флагов; запуск guest EN Android16 без crash;9 локалей, подпись/OAuth/Passkey allowlist, own Firebase resources и исходный AGPL в APK. PHP/JS contracts, native cookie/cache-isolation test,15 app guest routes401 и5 browser routes200 без maintenance/fatal. Реальное создание аккаунта/Passkey/вход/доставка FCM не заявлены. Локальные коммиты; push не выполнялся.

## Password login compatibility fix — 7 October 2026

The owner encountered a real password refusal. The exact company URL in the required User-Agent selects the engine's existing `$ourbrowser` password input mode; the server adapter now supplies its expected format instead of ROT13. Android consumes only302 Location=/ for login/passkey/device-verify without following it, then separately verifies cookie-backed admission. Wrong passwords and lockout remain authoritative and display bounded plain text from the engine.

Validation:43 social tests (0 failures/errors/skipped), own gplayDebug APK; backend actual-source Android/iOS password mode and wrong-password rejection, cookie isolation, device/2FA stage and Passkey signature/origin/replay checks; live guest session200, status401, invalid CSRF403 and foreign Origin403. APK installed/reopened on Android16; crash buffer empty. No account password was inspected or resubmitted, no lockout reset. Real-account success/visual acceptance remains with the owner. Existing form composition retained; specific errors replace the generic notice without duplication or HTML links. Local commit only, no GitHub push.

## Integrated social + Matrix checkpoint — 7 October 2026

Native SSO delegates PKCE/state and encrypted session storage to the original Rust SDK. Before session persistence/observers, account-bound OAuth verifies the source-provided permanent Matrix ID and revokes a mismatch. Logged-in and expired-Matrix-session flows retain independent social navigation. Cached Matrix remains available during social admission failures; explicit app logout synchronously hides account content and prevents restoration until a new admitted social login.

Social origin/Passkey RP migrated to antimsg.com. Agreement stays in-app with retry. Technical /go,/goto redirects are resolved inside with UA/cookie; only final external URL is opened outside. Legacy social requests are blocked, public outbound share URLs use the official web domain. Messenger UI/crypto/calls remain original pending design adaptation.

Validation: own gplayDebug and flags-off fdroidDebug;52 social,9 Matrix auth (including reject/revoke-before-store and normal OAuth flow),44 Firebase,291 push tests passed in both builds. Targeted ktlint passed. Real native SSO, bidirectional E2EE chat and social name projection passed earlier in the installed emulator build. Calls, Matrix push, final logout/relogin/consent, avatar/handle UI and recovery acceptance are still pending. APK/signing/credentials/QA data stay ignored. Shared docs record common iOS contract; no iOS build or GitHub push.

## Recovery-aware unified logout and social direct messages — 7 October 2026

Social Sign out with an active Matrix session now opens the original Element logout/secure-backup flow. No social cookie is cleared while that flow is cancelled or still saving keys. Only actual removal of the matching valid Matrix session closes social admission, push binding and pending routes; Matrix session expiration/recovery or removal of a different actor preserves social admission. Source actor is saved after a real admitted source response. Background cleanup uses the old cookie snapshot and never applies response cookies to a new login. Social-only logout stays immediate.

Profile Message/root Messages routes select native Messenger, resolve the numeric recipient through source account/block/contact checks and reuse/create an encrypted SDK DM. Pending routes are memory-only and actor-bound; loading can be cancelled, failures retried. Both own and flags-off builds passed58 social tests before the logout refinement; own logout refinement passed61 social+2 RootFlowNode tests,30 logout tests (one pre-existing ignored UI test),262 preferences tests. Real first SSO/E2EE/name projection were tested earlier; final recovery/consent/logout/DM tap/call/push UI acceptance remains pending while Mac is locked. No keys/credentials/QA data or GitHub push.


## Account recovery escrow — 7 October 2026

Owner approved recovery through the social account and maximum safe automation of technical UX. This is custodial recovery: the infrastructure operator can recover encryption keys. A separate AES-256-GCM vault on the messenger host stores a random passphrase per immutable source subject; the master key is outside Git/web, with a root-only backup on the separate social host. Source issues one-minute RS256 grants only after native transport, full cookie/device/2FA admission and CSRF. Purpose, operation, actor and single-use nonce are checked; no overwrite/delete API exists. Source secret never reaches web JavaScript or ordinary preferences/logs. Vault loss requires restoration of BOTH DB and master; an operational off-host DB backup schedule is still required before release.

Android saves escrow before SDK setup, restores via the original EncryptionService and verifies vault readback plus backup upload completion before simplified social logout. Failure retains stock safe logout; unknown existing recovery is not reset. Background recovery retries every five minutes without blocking chats/social pages. Own APK/ktlint/68 social+root tests pass, including four escrow ordering/account-isolation/upload failure tests. Server44 tests pass, including eight vault durability/replay/purpose/tamper/cache tests. Anonymous live vault returns401; source PHP lint passes. APK installed in the emulator. Real native recovery -> logout -> relogin -> old-message decryption remains unverified: computer-use cannot bind the standalone emulator window, and Android Studio Running Devices currently does not list it. Do not claim full application ready; calls/background Matrix push, account lifecycle propagation and final device acceptance remain outstanding. No GitHub push.


## Recovery vault backup and failure isolation — 7 October 2026

The pending off-host DB backup above is now implemented: an hourly consistent SQLite snapshot is exposed only to a forced read-only SSH command from the social host IP. Source pulls over a pinned host key using a dedicated export-only credential; retains14 daily snapshots in a root-only backup directory, with separate root-only master backup. Successful snapshot/pull/SQLite quick_check and forced-command negative test are verified; exporter cannot execute requested id. Snapshot runs as the vault owner to preserve WAL permissions. Failed vault master initialization returns503 only for recovery and preserves independent identity/discovery/health. Server45 tests pass (nine vault tests), source8 signed recovery and11 recipient contract cases pass; own build/ktlint/68 social+root checks pass. Background preparation stops on success or existing unknown recovery, retries failures every5min; it does not repeatedly download history after success. Audit has90-day/500000-record bounds. Actual native restoration and complete app acceptance still pending. No push.


## Account lifecycle and retained-store privacy — 7 October 2026

Source moderation now has its own InnoDB component outbox: global status, project deactivation, stable ban-until and sticky global-deletion tombstone. Source native handoff/recovery minting rechecks authoritative rows under short SQL locks; no HTTP while holding locks. Signed account events are durable/revision-bound and dispatched independently every15sec with a separate5min reconciliation. Native handoff also carries a fresh signed snapshot; recipient checks the newest local state at authorisation and code redemption. Retired IDs cannot be revived. Vault rejects an earlier recovery grant after a cached restriction.

Recipient applies reversible locks through MAS private admin listener8081 using a separate server-only client; preserves pre-existing manual locks/deactivation and expires owned temporary bans via timer. Atomic per-user non-secret policy files gate RTC even for previously issued OpenID and deny new managed-user room events in Synapse without consulting the source. All24 source snapshots accepted; mapped apply pending0. All services healthy;58 isolated server tests,10 real isolated source queue/fresh-admission cases,8 native recovery and11 recipient cases pass. Nginx gate tests and real peer joined/mismatch/nonmember/invalid/foreign checks pass. Actual24 policy files opened/validated at Synapse UID10001 with read-only mount; an earlier directory-list probe yielded0 because least-privilege ACL permits known-file reads, not directory listing, and was not counted as acceptance.

Android reuses an SDK-created backup only if the current device owns its key. Existing unknown recovery/backup is never replaced. Source account switching selects an existing matching SDK session or starts matching SSO, retaining the previous actor's encrypted store rather than logging it out blindly. Explicit social logout hides every retained actor; notification admission is checked before enqueue and after async resolution. Final notification posting and source-admission change/cancellation share a local lock. No secrets in these preferences. Own/original builds and72 social/root checks plus293 push checks pass. Final own build and appnav/push API+impl ktlint checks pass after constructor cleanup. Actual native recovery/logout/relogin/history, voice/video and background Matrix push remain unverified while macOS is locked (OS lock flag confirmed). This is a deployed development integration, not a claim of fully accepted app/iOS. No GitHub push.


## Verified automatic recovery navigation — 8 October 2026

After social-account escrow recovery succeeds, the own native flow waits for the SDK session verification status `Verified` and acknowledges the verification success step. Unknown/unverified sessions cannot be acknowledged. Notification permissions, screen-lock setup and other consent steps retain their own flow. Ordinary Element builds still use manual verification acknowledgement.

Actual emulator acceptance: native password relogin, automatic Matrix SSO, SDK verification/recovery and decryption of both previous encrypted test messages passed. Own gplayDebug build, eight DefaultFtueService tests and appnav/FTUE formatting passed. Full calls/background Matrix push/outage acceptance remains in progress; no release-readiness claim or GitHub push.


## Own analytics composition — 8 October 2026

`antintwrkNativeAuth=true` selects the upstream noop analytics service instead of Element Posthog/Sentry providers in the application dependency graph. This removes Element analytics consent onboarding and transmission providers from our app; flags-off builds retain the existing analytics configuration. Own gplayDebug and original fdroidDebug builds pass.


## First Matrix push opt-in and two-device RTC acceptance — 8 October 2026

A clean second emulator exposed an own-build defect: FCM auto-initialisation is deliberately deferred, but Matrix subscribed before the initial Firebase installation ID existed. The own provider now explicitly registers Firebase off the UI thread and waits up to 30 seconds for the stored installation ID before subscribing. Registration failure cannot subscribe a missing token. Successful explicit registration enables token maintenance independently of social push. Flags-off behavior is retained. The pre-existing gateway-test formatting was corrected. Own gplayDebug, all 46 Firebase module tests and module ktlint pass.

On the clean second device, native sign-in recovered the existing identity and encrypted history automatically without the manual verification or Element analytics screens. Following the fix, initial Matrix push registration succeeded without first opting into social push. A separate private encrypted QA peer sent a synthetic message while the app was backgrounded; Firebase delivery and the Matrix handler were observed, Android displayed the decrypted notification, and tapping it selected Messenger and opened the correct decrypted conversation.

Two distinct Android device sessions of the designated test account joined the private QA room call. Both received the other's synthetic-camera video through the own RTC/SFU endpoint; logs confirm encrypted audio/video subscriptions. Camera/microphone permission dialogs and explicit hangup worked. This verifies device-to-device encrypted media routing, not independent-user ringing, acoustic quality on phones or forced TURN relay. A transient missing-media-key warning during joining resolved; no encryption was disabled. The technical peer's private room needed explicit call-event power levels, which were applied only to that QA room; app-created rooms already configure these. Full final acceptance remains open for the other cases tracked in shared documentation. No GitHub push.


## Managed profile, recovery and logout - 8 October 2026

Own preferences route profile/account actions into native Social/Profile. Encryption settings show custodial status/retry without reset/disable. Both exit actions require escrow/readback, backup upload and SDK Verified before Matrix revocation. Failed remote logout preserves scope/store/delegate and permits retry/cancel. Own invalid Firebase payload history is redacted; stock flags-off paths remain upstream.

Own gplay build/changed-module ktlint, 46 Firebase tests and 8 Matrix client tests pass. Stock fdroid/preferences/Matrix checks pass. Actual social name edit appeared in Matrix without relogin; original QA name restored and observed in Matrix. Final managed settings exit reached welcome without manual keys. Shared docs/android-integration-report-2026-10-08.md lists remaining phone/provider/release acceptance; iOS not implemented. No push.


## Social navigation and DM lifecycle - 8 October 2026

Source message routes and validated legacy-room links open the main SDK DM. Own lookup ignores departed membership. Actor-scoped resolver serializes creation, reuses encrypted joined/invited conversations, replaces a departed recipient, and fails safely on ban/membership lookup outage/unencrypted or group rooms. Participant cards expose social profiles by immutable ID; own share links use public social IDs. Message and thread social content links stay in the native shell via an optional composition-local host handler; stock defaults retain external link behavior. Physical UI and test evidence, old-web store separation and remaining distributed creation/phone/provider acceptance are in shared docs/social-messenger-navigation.md. No push.

## WebView scale parity — 8 October 2026

Social WebView honors the page viewport (useWideViewPort/loadWithOverviewMode), starts with textZoom=100 to match the site's CSS/PWA baseline, and supports pinch zoom without legacy floating zoom buttons. Native text still follows Android accessibility font settings. No CSS font-size reduction or global screen-density override is applied. This deliberately normalizes only embedded web text; larger system-font users can zoom web content manually. iOS must use the page viewport and avoid an extra automatic text/page zoom layer when implementing the same contract.

Own debug build and WebFeed tests/ktlint pass; Android emulator feed renders within the content width. Exact visual parity on the owner's phone remains to be confirmed using the updated APK.

## Automatic recovery monitoring — 8 October 2026

The startup recovery task previously stopped after prepare returned false as well as after success. Android now retries failed/false outcomes with 30s/60s/120s/240s/300s capped backoff, rechecks healthy sessions every five minutes and reacts to SDK Incomplete/Disabled changes after setup. Actor-bound admission and immutable escrow remain required; unknown pre-existing keys are never reset. SDK cancellation propagates. Simplified logout additionally waits for RecoveryState.ENABLED after backup upload completion.

Own home banners no longer request a manual recovery key: they explain that backup is not yet confirmed and offer an app-managed status check. Stock Element text is preserved in flags-off builds. Nine social languages are included. The notice is not a claim that backup succeeded. This does not determine the exact cause on the owner's phone from a screenshot; real phone retry/status still needs confirmation.

Validation: nine account-recovery cases, three monitoring cases and 15 RoomListPresenter cases pass; own build/ktlint pass. iOS must implement equivalent monitoring rather than a one-time startup check.

## Уточнение интерфейса — 8 октября 2026

Владелец отменил увеличение WebView жестами: единый CSS-масштаб с textZoom=100, supportZoom=false/builtInZoomControls=false. Прежнее предложение pinch zoom больше не действует. Нативный размер шрифта сохраняет системную доступность.

При раскрытии редактора публикации, отдельной странице hero-compose, открытии создания истории/рилза нижнее меню скрывается; закрытие редактора возвращает его. Exact-origin/main-frame web-мост передаёт только Boolean-сигнал режима. Системная кнопка Back закрывает inline-редактор через штатный обработчик веб-части; форма сохраняет существующие правила черновиков. Аватар вкладки профиля берётся из cookie-authenticated social status, изображения ограничены размером и техническим origin; если фото отсутствует, показываются инициалы. Выход перенесён из отдельной нижней строки в меню «⋮».

Поиск людей использует индекс имён/коротких адресов соцсети, её видимость и блокировки; допуск личных сообщений проверяется отдельно перед созданием нового личного Matrix-чата; локальная привязка каталога проверяет social/Matrix actor и отбрасывает ответ после смены аккаунта. История Matrix и хранилище соцсети остаются независимыми; при недоступности социального каталога остаётся штатный Matrix-поиск. Поиск не переносит старую переписку и не создаёт Matrix-аккаунты из поисковых запросов.

Контракт относится к Android/iOS; код iOS ещё не реализован. Обновлённые результаты проверок фиксируются после сборки и UI-проверки.


Проверка этой доработки (8 октября): собственная Android-сборка и flags-off сборка Element успешны; 87 Antintwrk appnav cases, 9 RustMatrixClient cases и 6 JS editor cases прошли, PHP 7.3 lint успешен. Read-only SQL проверил имена, @короткий адрес, пустой и wildcard-like запросы. В эмуляторе поиск Maxim нашёл профиль соцсети; раскрытие/закрытие inline-редактора, отдельная страница публикации, окна рилза и истории скрывают/возвращают нижнюю навигацию. Back закрывает окно рилза через штатный Close. Меню содержит Sign out, отдельной строки внизу нет. Тестовый профиль без фото показывает инициалы; отображение изменённого реального аватара на физическом телефоне ещё не проверено. Публикации/рилзы/истории в этой проверке не отправлялись. iOS пока содержит согласованный контракт, без реализации этих изменений.

Каталог доступных для поиска людей не ограничивается возможностью написать им: запрет личных сообщений проверяется при создании нового DM. При отказе или недоступности соцсети новый mapped-social DM не создаётся; текущая история и существующие чаты продолжают работать. SQL каталога соответствует веб-поиску people.class.php; при изменении его правил оба запроса должны обновляться совместно.
