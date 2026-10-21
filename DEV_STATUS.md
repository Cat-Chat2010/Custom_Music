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
* **Поточна версія:** `0.0.2_NeoForge` (наступне збереження буде `0.0.3_NeoForge`).

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
  - Підпапки: `import/`, `tracks/`, `ffmpeg/`.
  - Автоматичне створення структури папок при запуску клієнта гри.
  - Сканування файлів імпорту з валідацією аудіорозширень (`mp3`, `wav`, `flac`, `m4a`, `aac`, `opus`, `ogg`).
  - Сканування готових `.ogg` треків.
  - Метод безпечного видалення треку з диска (`deleteTrackFile`).

### 3. Автоконвертер аудіо через FFmpeg (`com.cat.custommusic.audio`)
- [x] Клас `FfmpegConverter`:
  - Двоетапний пошук бінарника FFmpeg:
    1. Локально у папці `custom_music/ffmpeg/ffmpeg.exe` (Windows) або `ffmpeg` (Linux/Mac).
    2. У системному `PATH`.
  - Перевірка валідності через запуск `ffmpeg -version`.
  - Конвертація файлів у стандарт Ogg Vorbis:
    `ffmpeg -y -i <input> -c:a libvorbis -ar 44100 -ac 2 <output.ogg>`.
  - Безпечне видалення вхідного файлу з `import/` після успішної обробки.
  - Асинхронний метод `convertAllImportsAsync(...)` у фоновому потоці (`CompletableFuture`), що запобігає підвисанню інтерфейсу гри.

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
│   │   │   ├── FfmpegConverter.java         <-- Обробка аудіо через FFmpeg
│   │   │   └── MusicDirectoryManager.java   <-- Робота з папками custom_music/
│   │   ├── config/
│   │   │   ├── MusicConfig.java             <-- settings.json & синхронізація
│   │   │   ├── PlaybackMode.java            <-- Режими RANDOM / SEQUENTIAL
│   │   │   ├── TrackEntry.java              <-- Модель окремого треку
│   │   │   └── VanillaTracks.java           <-- Список ванільних мелодій гри
│   │   ├── gui/
│   │   │   ├── CustomMusicClientEvents.java <-- Додавання кнопки у SoundOptionsScreen
│   │   │   ├── CustomMusicScreen.java       <-- Головний екран налаштувань мода
│   │   │   ├── DelaySlider.java             <-- Віджет повзунка затримок
│   │   │   └── TrackListWidget.java         <-- Прокручуваний список треків
│   │   ├── mixin/
│   │   │   └── MusicManagerMixin.java       <-- Перехоплення ванільного MusicManager
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
└── DEV_STATUS.md                            <-- Цей файл (статус розробки)
```

---

- [x] Відображення затемненої плашки з першими 10 символами треку у правому верхньому кутку (`NowPlayingHud`).
- [x] Гаряча клавіша для швидкого виклику меню плеєра (налаштовується у розділі "Призначення клавіш", за замовчуванням `Ctrl + P`).
- [x] Кнопка швидкого відтворення та паузи `▶` / `⏸` навпроти кожного треку у списку.
- [x] Розміщення кнопки мода у порожньому слоті праворуч від кнопки "Диктор, голос" у `SoundOptionsScreen`.

---

## 🚀 Наступні можливі кроки (Беклог / Покращення)

1. **Текстурування та UI полірування:**
   - Створення кастомної іконки для списку модів (`logo.png`).
2. **Фільтри за ситуаціями/біомами:**
   - Можливість призначати кастомні треки конкретним вимірам (Nether, End) або біомам.
