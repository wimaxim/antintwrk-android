# Сохранение этапов разработки antintwrk

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
