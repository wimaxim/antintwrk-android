# Сохранение этапов разработки antintwrk

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
