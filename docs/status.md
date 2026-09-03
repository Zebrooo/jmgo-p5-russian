# Состояние проекта JMGO P5 Russian

Обновлено: 2026-09-03, ветка `claude/jmgo-p5-russian-dev-l8d4w1` поверх `main@9196a0c`.

## Что уже работает (по коду и локальным тестам)

- **input-core** (Java 8, без Android): фильтр кнопки микрофона (keyCode 609, только первый KEY_DOWN), debounce
  повторного нажатия, `VoiceSessionGate` с UUID и таймаутом 60 с, нормализация результата, выбор маршрута
  WEB/NATIVE по capability, политика выбора безопасного поля (не password, видимое, из исходного package).
- **web-input** (AAR): `WebInputController` с курсором (выключен по умолчанию, переключается кнопкой Menu),
  русской/английской/цифровой TV-клавиатурой, JS-мостом без передачи текста поля, вставкой через native value
  setter с `beforeinput`/`input`/`change`, Backspace, отправкой формы (`requestSubmit` → кнопка формы → Enter),
  очередью результата до `onResume`, проверкой безопасного активного элемента перед вставкой.
  `WebVoiceActivity` объявляет `org.jmgo.input.action.WEB_VOICE` и возвращает результат package-scoped broadcast.
- **zagonka-tv-wrapper**: WebView, allowlist домена, полноэкранное видео через `onShowCustomView`, Back с
  приоритетом «выйти из видео → история → закрыть», адаптер только с выбором первого результата поиска.
- **flauncher-jmgo**: домашний экран, Wi-Fi и батарея (MCU sysfs с fallback на `BatteryManager`),
  `NativeVoiceAccessibilityService` (`exported=true`, `BIND_ACCESSIBILITY_SERVICE`, «Русский голосовой ввод»),
  ожидание возврата исходного окна по событиям accessibility с повторной выборкой дерева, `ACTION_SET_TEXT`,
  `ACTION_IME_ENTER` только при поддержке узлом, capability-маршрутизация без списков package.
- **futo-voice-jmgo**: офлайн-распознавание (Vosk small-ru с fallback на Whisper) только для
  `org.futo.voiceinput.jmgo`, IME-поверхность в JMGO-сборке скрыта, `RecognizeActivity` завершает запись по
  broadcast `com.jmgo.action.AI_VOICE` или повторному нажатию микрофона. В логи попадают только длины и тайминги.

## Сделано в этой итерации

- Экран первоначальной настройки «Голосовой ввод» в лаунчере (Настройки → Голосовой ввод): статус FUTO,
  разрешения микрофона, сервиса специальных возможностей и домашнего экрана; кнопки перехода в нужные системные
  экраны; автопоказ при запуске, пока настройка не завершена (отключается переключателем); обновление статуса при
  возврате из системных настроек.
- Диагностика без записи речи: подключение сервиса, счётчики событий клавиш, время последней кнопки микрофона в
  сервисе и в лаунчере, исход последней сессии (только имя исхода).
- Иконка настроек на главном экране снова открывает панель настроек лаунчера (раньше панель была недостижима,
  внутри неё есть «Настройки Android»).
- Fallback кнопки микрофона в лаунчере: при выключенном сервисе открывается экран настройки вместо запуска
  распознавания, результат которого некому вставить. Удалён «слепой» запуск FUTO без получения результата.
- Доставка результата без фиксированной задержки 300 мс: `WebVoiceActivity` и `NativeVoiceCaptureActivity`
  шлют broadcast до `finish()`; web-хост держит результат до `onResume`, сервис — до возврата исходного окна.
- Debounce кнопки микрофона (350 мс) в сервисе, web-контроллере и лаунчере.
- Обход Accessibility tree ограничен 1500 узлами (BFS), неиспользуемые `AccessibilityNodeInfo` освобождаются
  на прошивках до Android 13.
- Тесты: устаревший UUID, повторный результат, таймаут, debounce (input-core); парсинг
  `enabled_accessibility_services`, политика fallback, диагностика (FLauncher Kotlin); страница настройки,
  автопоказ, канал, настройка (Flutter).
- GitHub Actions `ci.yml` для Node, Gradle/Zagonka, FLauncher (Flutter 3.7.5 + Gradle) и FUTO (NDK), корневой
  `scripts/build-all.sh`, README с порядком установки и удаления.
- Robolectric-тесты (запускаются в CI под AGP): манифест web-input резолвит `WEB_VOICE` на экспортированную
  `WebVoiceActivity`; `WebVoiceActivity` и `NativeVoiceCaptureActivity` шлют package-scoped broadcast с UUID,
  origin и первой непустой фразой, отказывают на кривой UUID и чужой action; `WebInputController` держит результат
  в паузе и вставляет после resume, отбрасывает небезопасное поле, чужую/устаревшую сессию, повтор, отмену и
  результат после навигации; манифест FLauncher: сервис `exported=true` с `BIND_ACCESSIBILITY_SERVICE` и meta-data,
  capture-активити не экспортирована, лаунчер не web-хост; `WebCapabilityResolver`; `AndroidEditableTarget`
  (фокус, fallback, password/hidden, лимит узлов, `ACTION_SET_TEXT`, `ACTION_IME_ENTER` только при поддержке);
  `<queries>` FLauncher и Zagonka.

## Что не реализовано

- Проверка на реальном JMGO P5 (P0): всё ниже основано на коде и JVM/Flutter-тестах.
- Instrumentation-тесты на устройстве или эмуляторе (Robolectric-покрытие есть, см. ниже).
- WebView: Shadow DOM, same-origin iframe, React/Vue controlled inputs без `beforeinput`, `InputEvent.inputType`,
  отказ от `document.execCommand` для contenteditable, реестр site adapters, адаптивные размеры клавиатуры под
  overscan, настройки скорости курсора, long press и drag.
- Удаление Firebase из лаунчера, обновление Flutter/AGP/Kotlin, подписанные релизы, таблица прошивок, аудит лицензий.
- Удаление legacy-кода старой JMGO-клавиатуры в FUTO (`JmgoKeyboard*`, `JmgoVoiceSession`) и неиспользуемых
  функций `VoiceSearchScript.forQuery`/`focusSearchField` в Zagonka.

## Найденные проблемы

- Панель настроек лаунчера была недостижима с главного экрана (иконка вела в Android Settings). Исправлено.
- Fallback лаунчера запускал распознавание при выключенном сервисе, результат терялся. Исправлено.
- Два теста `flauncher_test.dart` падали на `main` из-за изменённого app bar. Обновлены под JMGO-раскладку.
- `WebVoiceActivity` экспортирована без permission: любое приложение может запустить голосовую сессию в
  web-хосте. Вставка всё равно ограничена активным безопасным полем, но стоит добавить `android:permission`
  с `signature`-уровнем или проверять `EXTRA_ORIGIN_PACKAGE`.
- Сессия сервиса при аварийном завершении `NativeVoiceCaptureActivity` живёт до 60 с; всё это время кнопка
  микрофона только шлёт FINISH в FUTO. Безопасно, но заметно пользователю.
- Сборка FLauncher требует `google-services.json`; в CI без секрета используется заглушка (только проверка сборки).

## Риски на JMGO P5

- Прошивка может не доставлять keyCode 609 в accessibility key filter или перехватывать кнопку системным
  китайским пакетом раньше сервиса. Диагностика на экране настройки показывает, дошла ли кнопка.
- Стандартный `BatteryManager` может не публиковать аккумулятор проектора; используется sysfs MCU.
- `Settings.ACTION_HOME_SETTINGS` и `ACTION_ACCESSIBILITY_SETTINGS` могут отсутствовать в прошивке; предусмотрен
  fallback на общий экран настроек.
- Размеры WebView-клавиатуры фиксированы (412 dp) и не учитывают overscan.
- В песочнице разработки недоступны `dl.google.com` и Android SDK, поэтому локально проверены компиляция
  Java/Kotlin против `android-all` и JVM-тесты. Полные Gradle-сборки всех компонентов подтверждены в GitHub Actions.

## Результаты локальной проверки (2026-09-03)

| Проверка | Результат |
| --- | --- |
| DOM-тесты Node | 6/6 |
| input-core JUnit | 17/17 |
| web-input JUnit + Robolectric (CI) | 8/8 локально, 21/21 в CI |
| Zagonka JUnit | 8/8 |
| FLauncher Kotlin JUnit + Robolectric (CI) | 24/24 локально, 39/39 в CI |
| FLauncher `flutter analyze` | без замечаний |
| FLauncher `flutter test` | 142/142 |
| Компиляция Java/Kotlin против android-all 13 | без ошибок |
| Gradle `assembleDebug` всех компонентов | зелёный в GitHub Actions (run 33750692498, коммит `d96e96e`) |
