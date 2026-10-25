# 🛠️ Стан розробки мода (Developer & Project Status)

Цей документ призначений для відстеження прогресу розробки, архітектурних рішень та плану дій між розробником і асистентом.

---

## 📌 Загальна інформація про проєкт

* **Платформа:** Minecraft Java Edition
* **Версія гри:** `1.21.1`
* **Мод-завантажувач:** `NeoForge 21.1.65` (Java 21)
* **Тип мода:** Client-Only (тільки на стороні клієнта)
* **Головний Mod ID:** `custommusic`
* **Головний пакет:** `com.cat.custommusic`

### 🔒 Правила версіонування та Git (ЗАФІКСОВАНО):
1. **Збереження в Git:** Зберігати (комітити та пушити) **ТІЛЬКИ тоді, коли розробник явно говорить "збережи в гіт" / "запуш"**.
2. **Формат версій:** `0.0.X_NeoForge`:
   * `0` (перша цифра) — стабільна основа (не змінювати без прямої вказівки).
   * `0` (друга цифра) — бета-версія без багів.
   * `X` (третя цифра) — змінюється та інкрементується з кожним збереженням у Git (`0.0.1_NeoForge`, `0.0.2_NeoForge` і т.д.).
* **Поточна версія:** `1.0.0_NeoForge` (перший стабільний реліз, наступне збереження буде `1.0.1_NeoForge`).

---

## ✅ Що вже повністю реалізовано

### 1. Базова конфігурація та система збірки
- [x] Налаштовано `build.gradle` з плагіном `net.neoforged.moddev` версії `2.0.148`.
- [x] Налаштовано `gradle.properties` (версії MC 1.21.1, NeoForge, мапінги Parchment).
- [x] Створено `settings.gradle` з репозиторіями та плагіном Foojay resolver.
- [x] Підготовлено `gradlew.bat`, `gradle-wrapper.jar` та скрипт `build_mod.bat` (підхоплює Java 21 лаунчера).
- [x] Успішно протестовано компіляцію та збірку JAR (`custommusic-1.0.0.jar`).
- [x] Створено маніфест `src/main/resources/META-INF/neoforge.mods.toml` з client-only конфігурацією.
- [x] Налаштовано конфігурацію Mixin `custommusic.mixins.json` та зв'язано її з модом.

### 2. Файлова система та директорії (`com.cat.custommusic.audio`)
- [x] Клас `MusicDirectoryManager`:
  - Базова папка: `FMLPaths.GAMEDIR.get().resolve("custom_music")`.
  - Усі треки (`.mp3` та `.ogg`) зберігаються безпосередньо у `custom_music/`.
  - Автоматична міграція старих підпапок `tracks/`, `import/` та `ffmpeg/` в корінь `custom_music/`.
  - Автоматичне створення структури папок при запуску клієнта гри.
  - Сканування готових `.mp3` та `.ogg` треків.
  - Метод безпечного видалення треку з диска (`deleteTrackFile`).

### 3. Нативний аудіо-рушій без FFmpeg (`com.cat.custommusic.audio` & `mixin`)
- [x] Вбудована бібліотека pure-Java MP3 декодера JLayer (`javazoom:jlayer:1.0.1`), що шейдиться безпосередньо в JAR мода.
- [x] Клас `Mp3AudioStream` (реалізує ванільний `AudioStream` Minecraft):
  - Пофреймове декодування MP3 у 16-бітні Signed PCM буфери для OpenAL.
- [x] Клас `SoundBufferLibraryMixin`:
  - Перехоплення `SoundBufferLibrary.getStream(...)` для забезпечення OpenAL потоком `Mp3AudioStream` при відтворенні MP3 треків.
- [x] Повна відмова від FFmpeg — жодних сторонніх бінарників чи конвертації на диску не потрібно.

### 4. Конфігурація та серіалізація JSON (`com.cat.custommusic.config`)
- [x] Клас `MusicConfig`:
  - Зберігання налаштувань у `custom_music/settings.json` за допомогою `Gson` з форматуванням (`setPrettyPrinting`).
  - Параметри:
    - `playbackMode` (`RANDOM` / `SEQUENTIAL`).
    - `minDelaySeconds` (за замовчуванням 20 с).
    - `maxDelaySeconds` (за замовчуванням 60 с).
    - `tracks` (список об'єктів `TrackEntry`).
  - Метод `syncWithDisk(...)` — синхронізує файли на диску зі списком у конфігу, зберігаючи кастомний порядок і прапорці ввімкнення (`enabled`).
- [x] Клас `TrackEntry` — модель треку (`id`, `displayName`, `custom`, `enabled`, `fileName`).
- [x] Enum `PlaybackMode` (`RANDOM`, `SEQUENTIAL`).
- [x] Клас `VanillaTracks` — довідник стандартних фонових мелодій Minecraft (Overworld, Nether біоми, End, Меню, Творчий режим тощо).

### 5. Динамічний ресурс-пак (`com.cat.custommusic.resource`)
- [x] Клас `DynamicMusicPackResources` (реалізує `PackResources`):
  - Генерує віртуальний `sounds.json` у пам'яті для всіх `.ogg` файлів з папки `tracks/` з параметром `"stream": true`.
  - Віддає файлові потоки `IoSupplier<InputStream>` напряму з папки `custom_music/tracks/`.
- [x] Клас `DynamicMusicPackFinder`:
  - Реєструє пак через подію `AddPackFindersEvent` як обов'язковий (`PackSelectionConfig(required = true, TOP)`).

### 6. Менеджер черги та перехоплення фонової музики
- [x] Клас `CustomMusicPlayer`:
  - Відстеження завершення треків через `Minecraft.getInstance().getSoundManager().isActive()`.
  - Розрахунок випадкового таймера затримки тиші між треками.
  - Вибір треку відповідно до режиму `RANDOM` або `SEQUENTIAL`.
  - Пропуск вимкнених треків (`enabled: false`).
  - Підтримка програвання як кастомних аудіо, так і ванільних треків гри.
  - Можливість примусового пропуску треку `playNextTrack()`.
- [x] Mixin `MusicManagerMixin`:
  - Впровадження в `net.minecraft.client.sounds.MusicManager#tick()`.
  - Скасування стандартного ванільного таймера та делегування логіки в `CustomMusicPlayer`.

### 7. Графічний інтерфейс користувача (GUI) (`com.cat.custommusic.gui`)
- [x] Клас `CustomMusicClientEvents`:
  - Підписка на `ScreenEvent.Init.Post`.
  - Вбудовування кнопки `🎵 Кастомна музика...` в меню звуку (`SoundOptionsScreen`).
- [x] Клас `CustomMusicScreen`:
  - Кнопка зміни режиму `Випадково / По порядку`.
  - Кнопка «Імпортувати треки» (запускає конвертацію у фоні та оновлює ресурси гри).
  - Слайдери `DelaySlider` для мінімальної та максимальної затримки.
  - Кнопка переходу на наступний трек `⏭`.
- [x] Клас `TrackListWidget`:
  - Прокручуваний список треків (`ContainerObjectSelectionList`).
  - Чекбокси `✓ Вкл / ✕ Вимк` для кожного треку.
  - Кнопки переміщення вгору `▲` та вниз `▼`.
  - Кнопка видалення файлу з диска `✕` для кастомних треків.

---

## 🗺️ Карта файлів проєкту

```text
Minecraft-music/
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── src/main/
│   ├── java/com/cat/custommusic/
│   │   ├── CustomMusicMod.java              <-- Головний клас мода NeoForge
│   │   ├── audio/
│   │   │   ├── CustomMusicPlayer.java       <-- Логіка черги, таймінгів та відтворення
│   │   │   ├── Mp3AudioStream.java          <-- Pure-Java OpenAL PCM стрімінг для MP3
│   │   │   ├── MusicDirectoryManager.java   <-- Робота з папкою custom_music/
│   │   │   └── OggAudioHelper.java          <-- Обчислення тривалості OGG та MP3
│   │   ├── config/
│   │   │   ├── MusicConfig.java             <-- settings.json & синхронізація
│   │   │   ├── PlaybackMode.java            <-- Режими RANDOM / SEQUENTIAL
│   │   │   ├── TrackEntry.java              <-- Модель окремого треку
│   │   │   └── VanillaTracks.java           <-- Список ванільних мелодій гри
│   │   ├── gui/
│   │   │   ├── CustomMusicClientEvents.java <-- Додавання кнопки у SoundOptionsScreen
│   │   │   ├── CustomMusicKeyBindings.java  <-- Гарячі клавіші (Ctrl+P, Ctrl+O)
│   │   │   ├── CustomMusicScreen.java       <-- Головний екран налаштувань мода
│   │   │   ├── DelaySlider.java             <-- Віджет повзунка затримок
│   │   │   ├── NowPlayingHud.java           <-- Міні-плеєр HUD (постійний та спливаючий)
│   │   │   └── TrackListWidget.java         <-- Прокручуваний список треків з Drag&Drop
│   │   ├── mixin/
│   │   │   ├── MusicManagerMixin.java       <-- Перехоплення ванільного MusicManager
│   │   │   ├── SoundBufferLibraryMixin.java <-- Нативний стрімінг MP3 через OpenAL
│   │   │   └── SoundManagerMixin.java       <-- Безперервне звучання в меню
│   │   └── resource/
│   │       ├── DynamicMusicPackFinder.java  <-- Реєстратор ресурс-паку в NeoForge
│   │       └── DynamicMusicPackResources.java <-- Віртуальний sounds.json і стрімінг
│   └── resources/
│       ├── META-INF/
│       │   └── neoforge.mods.toml           <-- Метадані мода
│       └── custommusic.mixins.json          <-- Конфігурація Mixin
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew.bat
├── settings.gradle
├── README.md                                <-- Документація для користувачів GitHub
├── DEV_STATUS.md                            <-- Цей файл (статус розробки)
└── AGENTS.md                                <-- Постійна пам'ять агента (Antigravity memory)
```

---

- [x] Відображення затемненої плашки з першими 18 символами треку у правому верхньому кутку впритул до меж (`NowPlayingHud`, зменшений шрифт 0.5x, фіксований розмір).
- [x] Гаряча клавіша для швидкого виклику меню плеєра у розділі "Custom_Music" -> "Налаштування музики" (за замовчуванням `Ctrl + P`).
- [x] Гаряча клавіша для постійного міні-плеєра HUD (`Ctrl + O`).
- [x] Кнопка швидкого відтворення та паузи `▶` / `⏸` навпроти кожного треку у списку.
- [x] Безперервне відтворення музики в налаштуваннях та меню гри (`SoundManagerMixin`).
- [x] Нативний Windows Explorer діалог вибору аудіофайлів з безпечним копіюванням без видалення оригіналів користувача.
- [x] Двовкладковий інтерфейс (Альбоми / Налаштування) з закріпленим плеєром та слайдерами гучності.
- [x] Виправлено перехід до наступної пісні в альбомі після природного завершення треку у фоні (`lastPlayedTrack`).
- [x] Очищено нижню панель меню від перекриваючих статусних написів та дублюючої кнопки наступного треку; кнопку «Готово» відцентровано.
- [x] Нумерація неназваних альбомів числами `1`, `2`, `3`... (без слова «Альбом») з автоматичною міграцією.

---

## 🚀 Наступні можливі кроки (Беклог / Покращення)

1. **Текстурування та UI полірування:**
   - Створення кастомної іконки для списку модів (`logo.png`).
2. **Фільтри за ситуаціями/біомами:**
   - Можливість призначати кастомні треки конкретним вимірам (Nether, End) або біомам.
