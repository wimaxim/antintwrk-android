## Internal Google Play milestone —14 October2026

Owner-approved main-only production Passkey association applied14October: current com.antintwrk.app Play signer plus preserved debug signer, strict cryptographic/admission checks unchanged. Public canonical RP assetlinks verified200 and official Google DAL linked=true for credentials/links. Isolated signature/UV/challenge/counter/origin positive and negative tests passed; actual Play-installed Passkey/OAuth/FCM acceptance still separate. No APK rebuild for these two server metadata files. RU host metadata unchanged; see shared internal-release report for exact scope/rollback/hashes.

Shared report: `../docs/play-internal-release-2026-10-14.md` in the mobile workspace. Corresponding source `af55b4987a53726b5a61cfe01b306d02ba956ee1`; release1.0.37/code370 active only internally. Build/lintVital/bundletool/signature/manifest/64bit PT_LOAD/16KB ZIP passed; full16KB runtime remains pending. Distribution certificates require separate Google/passkey/push acceptance. Ordinary reviewer actors use normal social transport; private credentials stay out of Git in Console Sign-in details. No reviewer-only flags/admin privileges/security bypass. Public review, Billing activation and declarations remain separate gates; iOS requires own identities/acceptance.

# Сохранение этапов разработки antintwrk

## Network geo refresh and project notices — 10 October 2026

NetworkCallback monitors default network/link/VPN changes (debounce 2.5s); foreground/resume and periodic checks use a five-minute cache with bounded scheduling. Geo and routing admission use fresh sockets after network changes. Only prepare in the background; apply a same-actor/epoch, three-minute pending route on safe GET navigation or failed transport without active/hidden drafts, keyboard, chooser, modal or billing activity. Do not replay POST, reset Messenger or limit existing counters/push. Name in licences is multiline under project visionary/founder, with unchanged upstream copyright/AGPL and explicit independent-server scope. See shared docs/geo-network-refresh-2026-10-10.md for current contract, verification and iOS parity. Public source matching the APK remains a release gate; no push.

## Географический социальный origin — 10 октября 2026

Use the root-owned `AntintwrkWebFeedController.origin` for social page URLs, active bridge trust and resource guards. `BuildConfig.ANTINTWRK_SOCIAL_ORIGIN` remains the Russian native auth/default entry, not the active page origin. Bootstrap is only antimsg.com/app/geo; RU/unknown choose antimsg.com, other countries antintwrk.com. Apply before publishing initial admission; retain the origin/documents on resume and mirror changes into shared Android/iOS documentation.

Keep native auth/passkey RP and all messenger transports stable. Each origin owns its cookies/CSRF; a native one-use receiving-cookie-bound server grant transfers admission. Never copy raw cookies to the other host or pass secrets to JS. Logout clears both social origins, invalidates delayed writes and retains the existing SDK logout/recovery contract. Public sharing always uses antintwrk.com. Actor changes hide old content until the new origin is ready. Implementation and verification: ../docs/geo-social-routing-2026-10-10.md in the shared mobile workspace. Backend source stays outside this client repository. Physical-phone passkey and complete Russian new-device email verification remain separate acceptance; no push.

## 9 October 2026 — Play Billing foundation and project notices

Gplay-only Billing 9.1.0 is connected to the native activity lifecycle; product/ownership query APIs have bounded timeouts. This prepares integration, but paid checkout and server verification are not active: blue-badge integration waits for the parallel web implementation. Owner clarified Kindness purchases in Android also use Play Billing; independent Ukraine/David assistance keeps third-party providers without purchasing app points/features.

Licences start with Super David Universe LLC as operator, Maxim Novichkov as project author (stable social profile 2266620), independent original server/web rights, then client AGPL/upstream/dependency notices. Native integration remains covered client code. See [PROJECT-NOTICES](../PROJECT-NOTICES.md) and [shared milestone](../../docs/play-billing-foundation-2026-10-09.md) for actual validation and uncompleted Console/server/source-publication work. All nine supported locales are updated. No push or Drive replacement.


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


## Переустановка и неполная custody — 8 октября 2026

Обнаружен конкретный дефект ранней настройки: escrow-passphrase аккаунта 18 совпадал с текущим SSSS descriptor (проверен PBKDF2/HMAC), но три m.cross_signing.* secrets отсутствовали в Matrix account data. Public signing identity уже существовала. Поэтому повторный вход не мог полностью восстановить подписи. По прямому поручению владельца выполнена разовая операторская починка этого подтверждённого дефекта: создан новый комплект **служебных cross-signing подписей**, закрытые seeds сохранены под существующим SSSS key, public keys заменены штатным Matrix API. Это смена signing identity для данного legacy-аккаунта, а не восстановление потерянных signing seeds. Не менялись default SSSS key, passphrase vault, descriptor, m.megolm_backup.v1 и private/public backup key. Резервная копия дополнительно подписана новой master подписью. Проверено отсутствие изменения количества сохранённых room keys (на момент починки у аккаунта 18 их было 0); восстановление его старой переписки на физическом телефоне ещё не подтверждено. Временные maintenance compat-сессии отозваны через MAS admin finish, осталось 0. Запущен encrypted vault snapshot.

Operator-only scripts/repair-incomplete-recovery.py не подключён к публичному API или автоматической очереди. Guard требует подтверждённого соответствия escrow и активного backup, отсутствия ВСЕХ signing secrets и явного ограниченного набора переустановленных устройств. При существующих signing secrets/неизвестных устройствах отказывается от починки. Seeds хранятся только AES-GCM sealed в vault journal; частичная операция требует проверки оператором. Не применять к произвольным неизвестным ключам или аккаунтам.

Android теперь ждёт waitForE2eeInitializationTasks перед escrow/setup; новый secret storage создаётся только после SDK VerificationState.VERIFIED. Own restore вызывает SDK recover, а не recoverAndFixBackup: несовпадающая/недоступная backup не удаляется и не пересоздаётся. Own Import error больше не превращается в успех. Подтверждение backup/recovery/session для выхода сохраняется. Manual key banners отключены для account-managed сборки; это не подменяет RecoveryState.ENABLED/Verified и не отменяет retry monitor. Stock Element сохраняет исходный сценарий.

Реальная проверка: полностью удалено QA-приложение (со всеми локальными данными), заново установлено, выполнен нативный вход test1. SDK сообщил Account recovery ready; без ручного ввода ключа открылась прежняя комната, расшифрованы сообщения предыдущего дня и сегодняшние encrypted сообщения. Уведомления Android требуют обычного системного разрешения. Проверка проведена на Android-эмуляторе; аппаратный Pixel владельца не управлялся. Контракт перенесён в правила iOS, реализация iOS ещё не выполнена.


Проверки исправления: 11 AccountRecovery cases прошли, own RoomListPresenter — 14 passed/2 stock-only skipped, flags-off — все 16 passed; обе сборки успешны. Синтетические проверки operator crypto: secret roundtrip, wrong-name binding и MAC tamper rejection passed; повторная починка уже исправленного аккаунта отвергнута до мутаций. На момент проверки у владельца 0 joined rooms, поэтому доказательство расшифровки старой истории относится к тестовому аккаунту. APK account-recovery сохраняет поддержку social search/аватара/фиксированного масштаба/режима редакторов предыдущего этапа. No push.


## Matrix logical realm migration — 8 October 2026

Future logical IDs use antintwrk.com, independently of native Russian transport under antimsg.com. AntintwrkMatrixIdentity accepts public and legacy canonical IDs during migration only; immutable social number does not make two different Matrix IDs the same session/crypto identity. Keep exact actor checks and same-realm DM/profile routing; reject arbitrary domains. Server/SDK cutover, crypto/history and browser disclosure acceptance remain pending; see shared ../../docs/matrix-domain-migration.md in the parent mobile workspace. Do not publish a migration APK or switch source admission merely because the parser accepts the new domain. Local commits only.


## Account switching, preloading and shared settings - 8 October 2026

The authenticated social actor selects the full messenger ID. Scope pending OAuth/UI state to that actor, clear old social WebViews/routes/push bindings on account changes and never mix or delete another account's keys. Renew expired SDK metadata only after validating the new actor; ignore late callbacks for the previous actor. Start hidden OAuth and compose Messenger behind Social immediately after completed social admission. Preserve the Messenger tree across tab switches; first admission opens Social. Keep origin/callback/CSRF/actor validation and bounded waits with automatic retries.

Probe social and messenger availability independently in foreground: Android HEAD without credentials, 5-second timeout, 60-second healthy interval /15 seconds after failure, two consecutive failures to mark unavailable and one success to restore. These probes do not certify RTC/vault/backend health. Distinguish no internet from service unavailability; do not claim scheduled maintenance without an explicit signal. Show localized temporary-unavailability text, automatic/manual retry and access to the other tab. Web automatic recovery uses GET, never replays form/payment POST or reloads an active composer.

Shared Settings are accessible from both modes. Overflow has Create publication, People and Settings; Settings groups Account, Participation, Help/information and Sign out. Language applies to native resources and web routes: device default or EN/RU/UK/DE/ES/FR/IT/AR/ZH. Android persists/recreates Activity; iOS must follow this contract when implemented, but is not implemented in this change. Actual checks and limits: docs/android-account-switch-2026-10-08.md in the shared mobile project. Local commits only; no push.


## Social surface and admitted history - 8 October 2026

Keep social WebView/admission/navigation at Root lifetime across messenger handoff; clear them on logout/actor changes and rebind asynchronous callbacks. Managed personal-chat recovery may use missing admitted room keys rewrapped into the existing recipient backup by the private custodial worker. Never reset identities, bypass encryption/trust or imply success from hiding an undecryptable placeholder. Android retries unresolved sessions with bounded delays and cancels before timeline disposal. Same contract for iOS. Evidence/limits: shared docs/feed-and-history-fix-2026-10-08.md. Local commits only.


## Chat requests, unread messages and offline email — 8 October 2026

Show a localized “Chat requests (N)” heading immediately above incoming invitations. Count invitations from the complete locally synced room list, rather than a paginated/filtered screen. The top Messenger tab shows the sum of SDK unread **messages** in joined non-space rooms, including muted rooms; exclude invitations, departed rooms and duplicate summaries. Hide zero; display 99+ above 99 with the exact accessible count. Bind the counter to the active social/Matrix actor and reset it on actor changes.

Retaining a messenger conversation for background sync must not mark hidden messages read or auto-scroll its saved viewport. Provide a stable observable visibility state through retained/movable navigation content; allow automatic receipts only while the conversation is visible. Restore receipt handling when Messenger becomes visible, preserving scroll position. Android implemented; iOS must implement the same visibility and account isolation contract.

Offline DM email is generated server-side from encrypted-event metadata, never by a mobile client or from decrypted message text. The source host checks its stored account language and notify4 preference, fresh account status and social activity; a restricted, pinned source-IP-bound SSH command supplies Matrix member IDs, receipts and recent connection/presence timestamps. No general shell/database access is granted to this feed key. Sender name appears only in the localized body; no message preview, ciphertext, recovery secret or antimsg.com URL enters mail. Use the existing branded template and shared sendnotifymailpool()/mailingqueue/main SMTP route.

Process events after 60 seconds, with an independent 30-second timer. Social online window is the existing five minutes; Messenger uses three minutes across devices. Require offline at message time and at processing, suppress messages already read or recipients returning after arrival. Failure to determine presence defers the entire cursor rather than treating unknown as offline. Limit email to one per sender/recipient per 15 minutes. Initial cursor begins at current history; existing old test messages are not mailed. Cursor, cooldown and shared mail enqueue commit atomically; no second SMTP outbox. Sender/recipient must be active social accounts in a current two-person joined/invited DM. Groups and visible relation edits/reactions are excluded. Encryption does not disclose the inner event type; encrypted activity without a visible relation is indistinguishable from a message at this boundary. Public browser Messenger remains the app-availability notice.


## Resume, navigation and caching — 9 October 2026

Owner approved the contract with one correction: preserve all existing background requests, counters, SDK sync, push and recovery. Do not throttle these to speed up visible pages. Returning from another app preserves the existing page, scroll and input. Transient revalidation failure retains only an already admitted surface; initial admission and explicit authorization denial still require server confirmation. Logout/actor changes destroy previous social pages.

Ordinary top Messenger selection prepares all Chats before exposing it, clears list/space filters and search and removes internal screens without replacing the SDK session. Explicit recipient/notification routes remain specific destinations; profile return retains Messenger context. Cancelled or hidden navigation cannot override a newer intent or mark a hidden previous chat read. Keep the current Spaces name.

Native navigation starts loading before the HTTP response. An already retained tab opens without a new request. A deliberate downward drag at document top refreshes through GET with document revalidation; never replay POST. Editors, input, nested scrollers, dialogs, file selection and fullscreen video retain their gestures. Feed retains the existing new-publication counter/show button, without automatic page reload or scroll reset.

Retain the three main WebViews in memory. Private app HTML/API stay private,no-store. RU ingress applies browser cache only to an explicit allowlist of existing public JS/CSS: date-versioned v URLs one year immutable (change URL whenever bytes change); unversioned one hour. Unknown assets/dynamic/auth/mutation routes receive no such rule. Images/fonts retain source HTTP headers; proxy cache remains disabled and calls are unchanged. Android implementation/evidence: [report](../../docs/navigation-cache-2026-10-09.md). iOS must implement this contract later; it is not an iOS completion claim.


## Keyboard navigation and public asset cache — 9 October 2026

This supersedes editor-based lower navigation hiding and the earlier year/hour client-only cache. Hide the social native bottom navigation only while the system on-screen IME is visible; restore it when the IME closes, including while the editor remains open. Hardware keyboard focus alone does not hide it. Keep fullscreen-video suppression. Android uses Compose WindowInsets.isImeVisible for WebView and native fields; no JS focus heuristics, layout-height guesses or page reload. iOS must implement equivalent system keyboard visibility when developed. Editor state still prevents accidental pull refresh.

RU nginx now uses a bounded 10 GB public asset disk cache plus client headers: ?v=VERSION (nonempty safe identifier) 24 hours, without version 6 hours. JS/CSS, JPG/JPEG/PNG/GIF/WebP/AVIF/SVG/ICO/BMP/TIFF, PDF, video/audio and fonts are extension candidates; cache only matching resource MIME and successful responses. Preserve private/no-store/no-cache, Set-Cookie, sensitive Vary and X-Accel-Expires denial. Authenticated or cookie-dependent media bypass shared caching; known public project directories are cookie-independent. Range requests stream through with BYPASS, not stored as partial files. Dynamic HTML/native API remain uncached; background requests/counters and calls are unchanged. Version changes must use a new URL. See keyboard-cache-2026-10-09.md for verification.


## Resume navigation errors — 9 October 2026

A failed Messenger/recipient/external-link navigation must not display a global blocking dialog over a working social page. Show a localized, dismissible, short-lived snackbar instead; Retry is available only for an active explicit recipient request. Stop/account changes/new navigation invalidate old error completions, cancel the in-flight user navigation and clear its busy/error UI. Background sync, counters, recovery and admitted session checks remain unchanged. Do not reload social pages on resume. Preserve an explicit queued recipient request for controlled foreground completion; compare-and-set consumption must not clear a newer route. Android implemented; iOS must follow this contract when developed. See resume-errors-2026-10-09.md for actual verification and limitations.


## Silent retry before navigation errors — 9 October 2026

Before reporting a transient navigation read failure, retry up to five total attempts with 1/2/4/8-second delays, then show the nonblocking localized error. Preserve the page, scroll and input; do not reload the entire social document. This applies only to the non-mutating messenger-recipient lookup and /go,/goto redirect resolution. Never replay message sends, room creation, auth mutations or forms. AUTH_REQUIRED/CONTACT_UNAVAILABLE, rate limits, TLS trust errors, malformed responses and cancellation are not retried. Stop/new intent invalidates retries. Recipient lookup has an 8-second per-call timeout; background counts/sync retain their existing policy. Android implemented; iOS must apply the same safe-read-only rule when developed. See navigation-retry-2026-10-09.md.


## 9 October 2026: retained tab roots and preload

Native bottom-tab selection restores its root when a retained route differs. Profile/Communities preload once after first successful Feed visual readiness; hidden callbacks cannot alter the visible page. Local Chats navigation accepts outgoing DESTROYED targets and retries bounded preparation timeouts without key resets/API mutations. 119 appnav antintwrk tests, appnav/home ktlint and signed gplayDebug build pass. Full evidence: ../docs/tabs-preload-2026-10-09.md in the mobile documentation checkout. No push.


## 9 October 2026: keyboard-safe social editing

Active exact-origin social documents receive an app-local IME/viewport correction: reveal compact editors or follow the focus caret in long content, without reloading or changing draft/selection. Hidden pages cannot trigger corrections. 121 antintwrk appnav tests and 8 Node viewport tests pass; ktlint/build/signature/install pass. Emulator remained in floating Gboard mode; real-phone docked-keyboard acceptance is recorded as pending in the shared keyboard-editor-2026-10-09.md report. No push.


## 9 October 2026: embedded Messenger entry

Managed sessions create Home independently of stock FTUE completion and migrate saved FTUE/DESTROYED Home roots. Resolve the attached child by active NavKey, not the first matching target. Authentication/SDK verification/recovery/backup/logout checks are unchanged. Four BackStack regressions plus the full antintwrk appnav suite pass (125 cases); ktlint and signed build pass. Shared report: messenger-entry-2026-10-09.md. No push.

## 9 October 2026: unified app preferences

Messenger own-avatar and gear open main app settings. SDK media/PIN/location/notifications/moderation screens reuse the authenticated session in a fullscreen overlay, returning to common settings. Managed roots omit Appearance/Encryption/Labs/About/Developer options/version unlock/separate sign-out. Bug reports route to embedded social Support. Main logout binds a stored live matching SDK session before attempting coordinated revoke; failure preserves sessions. Version is shown only once, from actual APK metadata. MainActivity application lock covers both social and messenger. Shared report/index: ../docs/unified-settings-2026-10-09.md and ../docs/mobile-adaptation-index.md. Tests/build/manual outcomes are recorded there. No push.

Validation: native appnav 128 + preferences 87 + secondary logout 5 = 220 tests, no failures/skips; five changed module ktlint checks and signed gplayDebug pass. Flags-off fdroidDebug also assembles; secondary logout suite has 4 passes and one explicitly native-only skipped test. API 36 UI confirms shared avatar/gear entry, useful screens/Back, embedded Support, branded default sound, one APK version and coordinated logout with welcome retained after force-stop/start. PIN was not set during QA; Forgot PIN safety is tested through its LogoutUseCase. APK .8 / arm64 202610192; delivery/report in shared documentation. No push.

## 9 October 2026 — Hero World external SSO

Current .8 APK already resolves authenticated antimsg /goto and stops before the external SSO URL. Source v3 native-game-browser ticket fixes the cross-UA/IP browser handoff; ordinary web fingerprint preserved. Production server-side HTTP fixture test confirms correct Test Android actor cookie/game page and rejects replay without cookie. No native code/APK change; phone/browser UI acceptance separate. Mandatory shared report in the mobile root: docs/hero-world-handoff-2026-10-09.md; iOS carry-over in shared ios.md and adaptation index. No push.

## Release candidate preparation — 9 October 2026

Three retained social roots plus an additional internal WebView, native Play store bridge/ownership/restore, foreground + lock gate, Kotlin 2.4.20/BOM, hash-pinned false-CPE corrections and dedicated release signing/Firebase prepared. Client adapters are included under docs/social-web-adapter. 146 managed/Billing tests, formatting, 116 pure and 36 temporary-ledger server checks, 72 native-language renders passed. Console/product/signature/internal-track acceptance and source publication are distinct pending gates; registration not submitted, no push. See [shared report](../../docs/play-software-preparation-2026-10-09.md).

## Server badge and receiving Android Share — 10 October 2026

Accepted: show R after the displayed app version when the actually applied social origin is https://antimsg.com, G when it is https://antintwrk.com. This is the social content route, not a country label or messenger host. A prepared but unapplied geo change does not change the letter. Messenger/native authentication routes remain independent.

Android ACTION_SEND/ACTION_SEND_MULTIPLE target the social publication composer in native-auth builds; stock Element shares keep their existing messenger behavior. Accept plain text/URLs, JPEG/PNG/WebP images (up to 50, 20 MiB each) and at most one MP4/MOV/WebM/M4V video (500 MiB); total staging budget 550 MiB, text budget 10,000 characters. Unsupported/unreadable/empty/oversized input produces a localized explanation, never a partial publication. Advertise text, image and video MIME groups, not arbitrary files. The web composer has no general PDF/document attachment feature, so receiving those is not advertised.

Stage granted external content:// streams on an IO dispatcher into private cache under generated filenames; reject file/network/own-provider inputs and excessive count, enforce actual bytes even if provider metadata lies. Copying allows login without losing the sender URI permission. Pending content stays within the current process; after process death the sender can Share again. Discard account-bound pending content on logout or actor change. Login-required content has no actor until the first admitted account confirms it. Keep staged media readable while the web pipeline processes it; clean retained staging after 24h and when the current delivery is destroyed/logout occurs. Do not log shared text, paths, filenames or payloads.

Ask to create a publication as the admitted social profile; do not send to Matrix or publish automatically. Preserve live/hidden drafts and block importing over them. Open the separate /LANG/hero-compose additional surface without replacing Feed/Communities/Profile. Native injection is confined to the active admitted exact-origin main frame on that route, bound to the actor. Treat text strictly as text, never HTML/URLs to execute. Refuse a restored nonempty/non-post draft. A unique import ID prevents repeated insertion. Images/video enter the existing web file-input normalization/upload/session/CSRF/moderation pipeline via DataTransfer change events. A per-import random capability streams only staged files through WebView interception on the exact social origin: no HTTP traffic, cookies, base64 or generic filesystem access. The actor/page-bound lease expires after two minutes and is revoked on navigation/logout/completion; the namespace returns a local denial if no valid lease exists. Chromium does not allow automatic chooser opening without a web gesture, so native gallery import does not depend on opening another picker. Failure does not bypass web validation or submit the publication. Review/edit and explicit Publish remain required.

Source publication: owner explicitly requested public access to wimaxim/antintwrk-android through the in-app Source Code link. Publish the complete modified client and exact adapters with AGPL/copyright/notices and standalone build instructions. Include only public Firebase Android client configuration. Private signing/service accounts, backup/server source and QSNEmain remain excluded. This is a scoped publication instruction for the own Android repository; normal future push still requires a separate command. No upstream push, force push or iOS visibility change.

Import error follow-up: a failed incoming share keeps its failure explanation even when a composer is already open; only a genuine draft conflict uses the close-draft instruction. Targeted eight native-share tests and ktlint passed.

A stale failed staging read clears its own preparing request after an admission epoch change instead of leaving an indefinite preparing dialog. Eight sharing tests and ktlint passed.

## Mobile requirements, 10 October 2026

Own managed client adds gallery/system camera with bounded private capture output and actor/epoch/page-safe callback; cancellation/logout cannot complete a different account's picker. Explicit system Share creates a fresh UUID standalone composer instead of blocking an existing editor. Ordinary root drafts remain retained; open file/prompt/save/fullscreen actions finish before new share admission. Backend isolates native draft keys and uses standard engine mail; its independent private source stays outside this client repository.

Managed chat details hide Polls/Internal room ID/Room version even in debug. DM Profile opens the social resolver directly; DM Invite selects one phone contact then opens a translated system SMS draft with public antintwrk.com only. No contact upload, READ_CONTACTS or SEND_SMS, and no automatic send. Group member invitation keeps messenger user search. Monetization is above Settings in the social overflow; Referrals/extra participation category removed from common settings. Camera strings and SMS cover all nine social languages. iOS must reproduce this contract when implemented.

Validation: 169 appnav Antintwrk tests + 90 roomdetails tests; 11 final share/picker boundary checks; one actual API36 Chromium media-import instrumentation test; own signed APK upgrade. Full cross-platform contract and exact delivery evidence live in the owner's shared mobile documentation, docs/mobile-tz-2026-10-10.md. No push without the owner's explicit command; APK/runtime and physical camera/SMS acceptance are distinguished.


## Version 1.0.5, shared invitation and hidden personal chats — 10 October 2026

Common Settings owns Invite friends; DM details no longer expose SMS Invite. Accept the system-selected content URI rather than one fixed OEM authority; selected-number/SMS errors fall back to the share chooser. The human sends the SMS/share, with public antintwrk.com only and no contacts/SMS permissions.

Managed DM Delete chat confirms device-local hiding without SDK leave/forget, history/keys/receipt changes. Persist under the private session directory, filter before pagination with existing selected filters, exclude old hidden unread from the common badge, and restore on a new incoming message (including encrypted) or explicit ID route. Groups retain membership invitation/leave. Version is 1.0.5, without test, with existing R/G. GNU/upstream LICENSE files stay intact; PROJECT-NOTICES carries our rights/operator/founder.

Shared contract, iOS parity and actual limitations: ../docs/android-final-refinements-2026-10-10.md. Android validation: 263 passes and 2 existing stock recovery-prompt cases skipped in the native variant; no failures. PHP/Twig native-profile tests and admitted HTML on both origins pass. Physical OEM contact/SMS and two-phone receive/restore remain acceptance work; no SMS/payment/publication sent. Local commits only; no push.


## 11 October: DM actions and permission at entry

See [shared contract](../../docs/direct-actions-call-permission-2026-10-11.md). Personal-chat Invite/Share/People are suppressed by direct metadata before profile load; groups retain the actions. Group menu/wizard say New group chat in9 locales. RootView checks missing Android14+ full-screen call permission before social/Matrix preparation, with entry/return/rotation guards. Server counterpart m.direct is repaired without history/key reset. 54 Android checks and10 backend checks passed; Android1.0.16 is the test build. Backend module/installer stay outside public Android source. Commit locally; push remains command-only.


## 11 October: reciprocal personal-chat reply

[Shared contract and iOS port](../../docs/personal-chat-reply-2026-10-11.md). MATRIX_SPEC/direct metadata governs native DM actions, including pre-profile Settings and context menus; stale Leave events are blocked. Explicit social/search writing accepts matching incoming requests through one validation helper and reuses their ID. Account-data/lazy-room sync and acknowledgement guards prevent replacement creation in these routes. Android1.0.17;104 managed checks passed/2 stock recovery cases skipped. User messages/history/keys were not reset. Local commits only; no push.


## 11 October: social client version

[Shared version/reset contract and iOS parity](../../docs/client-version-and-reset-2026-10-11.md). Installed own version and ANTINTWRK_APP_VERSION use one Gradle helper; every social UA includes app/version while protocol1.0/company marker stay intact.48 auth/WebView/routing checks pass. Current room/history operator reset was explicitly authorized; identities and recovery preserved. Android1.0.18. [Remaining Play steps](../../docs/play-launch-plan-2026-10-11.md). Local commits only; push by command.


## 11 October: actual logout and startup minimum

[Shared cause, corrected cookie cleanup/admission isolation and iOS contract](../../docs/logout-startup-2026-10-11.md). Android1.0.19 removes Domain/Path/HttpOnly/SSO cookies from its private jar and gates new auth on completion, invalidates root-owned late results and preserves another actor from a late SDK logout. Visible startup animation is at least2s, background initialization retained and permission window deferred. No room/key reset or push.


## 11 October: pending message key indicator

[Shared UX and iOS parity](../../docs/message-keys-loading-2026-10-11.md). Managed encrypted waiting bubbles use localized inline progress; decrypt updates replace them without refresh, other messages/composer stay available and explicit trust/withheld errors remain. Android1.0.20. No server/key/history reset or push.


## Отправка и неподписанные устройства — 11 октября 2026

[Причина предупреждения, безопасный SDK-режим, Android проверки и перенос на iOS](../../docs/signed-device-sharing-2026-10-11.md). Managed client использует identity-based sharing: неподписанные устройства не получают ключи и не блокируют подписанные. Account recovery и gate собственной подписи сохранены; changed identity не обходится. История/vault/ключи/сервер не сбрасываются. Android1.0.21; результаты поставки в связанном документе.


## Повторный вход после очистки данных — 11 октября 2026

[Уточнение владельца, пробел ранней отправки, SDK verification gate и перенос на iOS](../../docs/fresh-login-send-gate-2026-10-11.md). Очистка данных на том же телефоне создаёт новую crypto session. Managed SDK outbox ставится на паузу до restore/UI, включается только после настоящего Verified и Running sync; соцсеть/история/recovery не сбрасываются. Не путать это с ручным добавлением физического устройства. Android1.0.22; результаты и ограничения проверок в связанном документе.


## Информационная подсказка восстановленных ключей — 11 октября 2026

[Общий Android/iOS контракт, причины и проверки](../../docs/account-recovery-notice-2026-10-11.md). В собственной same-realm social переписке без explicit forwarder убрана только grey/noncritical AuthenticityNotGuaranteed из badge/popup и long-press summary. Raw provenance и строгая SDK инспекция сохраняются; UI не объявляет backup-ключи Verified. Critical, mismatch, changed identity, device/cleartext ошибки остаются. Android1.0.23; ключи, сервер и текущие чаты не сбрасывались.

## Regional transport — 14 October 2026

Managed1.0.36 retains canonical HTTPS/SNI/issuer/RP/cookie/session scopes while routing new TCP streams through RF/LA entrances. Own loopback CONNECT handles only exact service hosts443; no TLS termination, arbitrary proxy, request logs, active-stream replay or retained-page reload. Anonymous regional Social/Matrix readiness and cooldown scopes are independent. Stock flags-off behavior preserved. See ../libraries/network/src/main/kotlin/io/element/android/libraries/network/antintwrk/ and appnav AntintwrkWebTransport.kt; shared ../../docs/messenger-ha-2026-10-14.md/runbook have platform parity and actual failure acceptance.8 TCP/route tests,33 selected auth/geo regressions, ktlint/build/signature passed; emulator Welcome/Login/guest Terms and LA private preference observed. Physical/account/calls/iOS acceptance remains separate; older WebViews without reverse-bypass retain legacy direct web routing. No identity/key/history reset, fake verification or hostname/issuer rewrite.
## Incoming call alerts — 14 October 2026

Managed authenticated unexpired SDK RING events alert immediately without requiring a room cache to catch up in3 seconds. Preserve expiry/force-notify/actor policy and encrypted SDK resolution; do not invent calls from raw push payloads or reset keys. Historical stored logins do not suppress callers in the single admitted actor app; own RTC events stay suppressed. Call lifecycle waits for observed active/answered membership before accepting absent cached state as hangup, independently per call. Avatar loading is bounded300ms; CallStyle/full-screen intent/ringtone/vibration remain on the high importance channel. Native root requests POST_NOTIFICATIONS on Android13+ once per installation, after the2s startup interval; denial does not loop/block login. Android14+ full-screen permission follows ordinary notification grant. Targeted push46/call25/root permission11 tests passed with native flags, plus module ktlint. Owner confirmed incoming ringtone/visible call on two physical phones in foreground and locked-screen modes on APK1.0.37 (14 October: works in both modes). Media quality, other lifecycle/network scenarios and iOS acceptance remain separate. Shared incident/release record: ../../docs/incoming-call-alerts-2026-10-14.md in the mobile documentation root.


## Play OAuth metadata repair — 14 October 2026

[Release38 configuration, Google signer association, reproduced rejection and validation boundaries](antintwrk-play-oauth-2026-10-14.md). Matched managed domain/redirect fixes release registration without relaxing MAS policy; ordinary API integration succeeds. Existing identities/history are preserved; physical Play-installed acceptance remains separate.
