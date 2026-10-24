# 🎵 Custom Music Mod for Minecraft 1.21.1 (NeoForge)

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/NeoForge-21.1.65+-orange.svg" alt="NeoForge">
  <img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Side-Client-purple.svg" alt="Client-Side Only">
</p>

---

## 🇬🇧 English

A comprehensive client-side music manager and player for **Minecraft 1.21.1 (NeoForge)**. It allows you to play your own custom tracks, manage playlists, and listen to all official Minecraft music with an in-game HUD mini-player and seamless background playback.

### 🤖 AI Development Disclosure
This project was developed by **Cat-Chat2010** with the assistance of **Artificial Intelligence (Google Antigravity / AI Pair Programming)** for code architecture, mixins, audio streaming engine, and documentation.

### ✨ What the Mod Includes

* 📌 **HUD Mini-Player (`Ctrl + O`):**
  * Compact on-screen overlay (96x34) in the top-right corner.
  * Real-time timeline: elapsed time, remaining time (`-X:XX`), total track duration, and a clean progress bar.
  * Smooth slide-in / slide-out animations when switching tracks or toggling the player.
  * Permanent display mode (`Ctrl + O`) or automatic 7-second popup on track change.
* ⚙️ **In-Game Settings Menu (`Ctrl + P`):**
  * **Native File Picker:** Click "Add Music" to open the standard Windows Explorer dialog. Multi-selection supported (Ctrl / Shift).
  * **Instant Preview:** Play and pause any track directly inside the list with `▶` / `⏸`.
  * **Drag-and-Drop Reordering:** Easily change track sequence by dragging items in the list.
  * **Track Toggle:** Enable or disable individual tracks with checkboxes.
  * **Silence Delay Sliders:** Configure minimum and maximum pause duration between tracks.
  * **Playback Modes:** Switch between Random shuffle and Sequential order.
* 🔊 **Uninterrupted Background Playback:**
  * Music continues playing without pausing or cutting off when opening the Pause Menu, Options, or mod screens.
* 🎶 **All 79 Vanilla Minecraft 1.21.1 Tracks Supported:**
  * Fixed sound paths for all official Overworld, Nether, End, Creative, and Menu tracks.
  * Music discs (`Pigstep`, `Cat`, `Otherside`, `Creator`, `Precipice`, `Relic`, etc.) play their exact song without random selection.
  * Dedicated color badges: `[Custom]` (Cyan), `[Vanilla]` (Gray), and `[Disc]` (Gold).
  * Verified composer attribution: Aaron Cherof (*Precipice*, *Featherfall*), Lena Raine (*Endless*, *Creator*), etc.
* ⚡ **Native MP3 & OGG Playback (No FFmpeg Needed):**
  * Direct playback of `.mp3` and `.ogg` files via embedded pure-Java audio decoding into Minecraft's OpenAL engine.
  * No external software, commands, or codecs required — works 100% out of the box!
* 🌐 **Full Multi-Language Support:**
  * Native in-game localization for English (`en_us.json`), Ukrainian (`uk_ua.json`), and Russian (`ru_ru.json`).
  * Automatic English fallback for all other languages selected in Minecraft.
* 🛡️ **User File Safety:**
  * Your original music files are never altered, moved, or deleted — only safe working copies are created.

### ⌨️ Controls
* **`Ctrl + P`** — Open Music Settings & Track List
* **`Ctrl + O`** — Toggle HUD Mini-Player

### 📂 Folder Structure
```text
.minecraft/
└── custom_music/
    ├── your_song_1.mp3   <-- Drop your .mp3 tracks here
    ├── your_song_2.ogg   <-- Drop your .ogg tracks here
    └── custommusic.json  <-- Configuration and playlist order
```

### 🎵 Adding Music
* **In-Game:** Press **`Ctrl + P`** and click **"📁 Add Music"** to pick files using the native file dialog.
* **Manually:** Simply copy any `.mp3` or `.ogg` audio files into `.minecraft/custom_music/`.

### 🔨 Building from Source
* Requirements: **JDK 21**
* Build jar:
  ```cmd
  gradlew.bat clean build
  ```
* Output jar is located in `build/libs/`.

### 📜 License
Distributed under the **MIT License**.

---

## 🇺🇦 Українська

Повноцінний клієнтський музичний менеджер і плеєр для **Minecraft 1.21.1 (NeoForge)**. Дозволяє додавати власні аудіотреки, керувати плейлистами та слухати всю офіційну музику Minecraft зі стильним HUD міні-плеєром і безперервним фоновим відтворенням.

### 🤖 Створено за допомогою ШІ (AI Disclosure)
Цей проєкт створений автором **Cat-Chat2010** за активної участі **штучного інтелекту (Google Antigravity / AI Pair Programming)** для розробки архітектури коду, міксинів, звукового рушія та документації.

### ✨ Що є в моді

* 📌 **HUD Міні-плеєр (`Ctrl + O`):**
  * Компактна плашка (96x34) у правому верхньому куті екрана.
  * Точний таймлайн у реальному часі: пройдений час, скільки залишилося (`-хв:сек`), загальна тривалість та смужка прогресу.
  * Плавна анімація (slide-in / slide-out) під час зміни треків або відкриття вікна.
  * Режим постійного закріплення (`Ctrl + O`) або автоматична поява на 7 секунд при старті нової пісні.
* ⚙️ **Меню налаштувань у грі (`Ctrl + P`):**
  * **Нативний вибір файлів:** Кнопка «Додати музику» відкриває системний провідник Windows Explorer. Підтримується вибір кількох файлів одночасно (Ctrl / Shift).
  * **Швидке прослуховування:** Кнопка `▶` / `⏸` біля кожної пісні для миттєвого тестування чи зупинки.
  * **Перетягування треків (Drag-and-Drop):** Зручна зміна порядку пісень мишкою прямо у списку.
  * **Вимкнення окремих треків:** Можливість тимчасово вимкнути будь-яку композицію чекбоксом.
  * **Повзунки затримки тиші:** Налаштування мінімальної та максимальної паузи між піснями у секундах.
  * **Режими відтворення:** Випадковий вибір (Random) або послідовне відтворення за списком (Sequential).
* 🔊 **Безперервне фонове відтворення:**
  * Музика не зупиняється і не ставиться на паузу при відкритті меню паузи, налаштувань гри чи меню мода.
* 🎶 **Підтримка всіх 79 ванільних треків Minecraft 1.21.1:**
  * Фіксовані шляхи до звуків для всього саундтреку (Overworld, Nether, End, Creative, Menu).
  * Музичні платівки (`Pigstep`, `Cat`, `Otherside`, `Creator`, `Precipice`, `Relic` тощо) грають свою точну мелодію без випадкового вибору.
  * Кольорові бейджі: `[Custom]` (блакитний), `[Vanilla]` (сірий) та `[Disc]` (золотий для платівок).
  * Точне авторство треків: Aaron Cherof (*Precipice*, *Featherfall*), Lena Raine (*Endless*, *Creator*) та інші.
* ⚡ **Нативне відтворення MP3 та OGG (без FFmpeg):**
  * Пряме програвання файлів `.mp3` та `.ogg` завдяки вбудованому Java-декодеру в OpenAL рушій Minecraft.
  * Жодних сторонніх програм, кодеків чи консольних утиліт — працює на 100% з коробки!
* 🌐 **Повноцінна багатомовна локалізація:**
  * Повна підтримка англійської (`en_us.json`), української (`uk_ua.json`) та російської (`ru_ru.json`) мов інтерфейсу.
  * Автоматичне використання англійської мови як резервної для будь-яких інших мов гри.
* 🛡️ **Безпека файлів користувача:**
  * Ваші оригінальні аудіофайли ніколи не видаляються і не змінюються — створюються лише робочі копії.

### ⌨️ Керування
* **`Ctrl + P`** — Відкрити меню налаштувань музики
* **`Ctrl + O`** — Увімкнути / вимкнути HUD міні-плеєр

### 📂 Структура папок
```text
.minecraft/
└── custom_music/
    ├── vasha_pisnya_1.mp3 <-- Сюди додаються ваші .mp3 треки
    ├── vasha_pisnya_2.ogg <-- Сюди додаються ваші .ogg треки
    └── custommusic.json   <-- Файл конфігурації та збереженого порядку
```

### 🎵 Як додавати музику
* **Прямо у грі:** Натисніть **`Ctrl + P`** та оберіть **«📁 Додати музику»** для вибору файлів через системний провідник.
* **Вручну:** Просто скопіюйте будь-які аудіофайли `.mp3` або `.ogg` у папку `.minecraft/custom_music/`.

### 🔨 Збірка з вихідного коду
* Вимоги: **JDK 21**
* Команда збірки:
  ```cmd
  gradlew.bat clean build
  ```
* Готовий файл `.jar` буде в папці `build/libs/`.

### 📜 Ліцензія
Поширюється за ліцензією **MIT License**.
