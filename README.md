# JMGO P5 Russian input suite

Набор исходников для русскоязычного интерфейса и ввода на проекторе JMGO P5.

## Состав

- `jmgo-input-modules/` — общий Android-модуль: маршрутизация микрофонной кнопки, безопасная работа с активным полем и web-клавиатура/курсор.
- `zagonka-tv-wrapper/` — TV-обёртка сайта «Загонка» на WebView. Использует общий web-модуль и небольшой адаптер сайта.
- `flauncher-jmgo/` — адаптированный FLauncher с системным мостом голосового ввода для нативных приложений.
- `futo-voice-jmgo/` — офлайн-распознавание русской речи. В JMGO-сборке собственная IME-поверхность отключена: нативные приложения сохраняют свою клавиатуру и интерфейс.

## Поведение

- На сайтах модуль показывает собственную TV-клавиатуру и курсор только по запросу пользователя.
- В нативных приложениях вроде VK Видео и SmartTube web-интерфейс не запускается.
- Кнопка микрофона вызывает русское распознавание, а результат вставляется только в активное редактируемое поле исходного окна.
- Поля пароля, выключенные и read-only поля игнорируются.
- Результаты распознавания привязаны к идентификатору сеанса и имеют тайм-аут.

## Сборка и тесты

Нужен JDK 17. Android SDK задаётся локальным `local.properties`, который намеренно не хранится в Git.

```bash
# Общий модуль и Загонка
./flauncher-jmgo/android/gradlew -p zagonka-tv-wrapper \
  :input-core:test :web-input:test :app:testDebugUnitTest \
  :input-core:assemble :web-input:assemble :app:assembleDebug

# DOM-логика web-модуля
node --test jmgo-input-modules/web-input/src/test/js/jmgo-web-input.test.js

# Лаунчер
./flauncher-jmgo/android/gradlew -p flauncher-jmgo/android \
  app:testDebugUnitTest app:assembleDebug

# Голосовой сервис
./futo-voice-jmgo/gradlew -p futo-voice-jmgo \
  testPlayStoreDebugUnitTest assemblePlayStoreDebug
```

Проекты FLauncher и FUTO Voice Input содержат собственные лицензии и сведения об исходных проектах. Секреты, подписи, локальные настройки, core-дампы и готовые APK в этот репозиторий не включены.
