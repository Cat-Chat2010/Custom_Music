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
* ⚡ **Zero-Setup OGG Playback:**
  * Direct `.ogg` file import without requiring FFmpeg.
* 🎛️ **Optional FFmpeg Audio Converter:**
  * Automatic background conversion for `.mp3`, `.wav`, `.flac`, `.aac`, `.m4a` when FFmpeg is available.
* 🛡️ **User File Safety:**
  * Your original music files are never altered, moved, or deleted — only safe working copies are created.

### ⌨️ Controls
* **`Ctrl + P`** — Open Music Settings & Track List
* **`Ctrl + O`** — Toggle HUD Mini-Player

### 📂 Folder Structure
```text
.minecraft/
└── custom_music/
    ├── import/        <-- Drop audio files here (.mp3, .wav, .flac, etc.)
    ├── tracks/        <-- Ready-to-play .ogg tracks
    ├── ffmpeg/        <-- Optional: place ffmpeg.exe here if not in system PATH
    └── custommusic.json <-- Configuration and playlist order
```

### 🛠️ FFmpeg Setup (Optional)
* **`.ogg` files and Minecraft Vanilla tracks:** Work out of the box with zero setup!
* **`.mp3`, `.wav`, `.flac`, `.m4a`:**
  * **Option 1:** Install to system PATH (e.g. run `winget install Gyan.FFmpeg` in Windows Terminal).
  * **Option 2:** Place `ffmpeg.exe` into `.minecraft/custom_music/ffmpeg/`.

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
* ⚡ **Робота OGG без налаштувань:**
  * Прямий імпорт `.ogg` файлів без потреби у FFmpeg.
* 🎛️ **Опціональний конвертер FFmpeg:**
  * Автоматична фонова конвертація `.mp3`, `.wav`, `.flac`, `.aac`, `.m4a` за наявності FFmpeg.
* 🛡️ **Безпека файлів користувача:**
  * Ваші оригінальні аудіофайли ніколи не видаляються і не змінюються — створюються лише робочі копії.

### ⌨️ Керування
* **`Ctrl + P`** — Відкрити меню налаштувань музики
* **`Ctrl + O`** — Увімкнути / вимкнути HUD міні-плеєр

### 📂 Структура папок
```text
.minecraft/
└── custom_music/
    ├── import/        <-- Сюди можна скидати аудіо (.mp3, .wav, .flac тощо)
    ├── tracks/        <-- Готові до відтворення .ogg треки
    ├── ffmpeg/        <-- Опціонально: сюди можна покласти ffmpeg.exe
    └── custommusic.json <-- Файл конфігурації та збереженого порядку
```

### 🛠️ Налаштування FFmpeg (Опціонально)
* **`.ogg` файли та ванільна музика:** Працюють одразу без додаткових дій!
* **`.mp3`, `.wav`, `.flac`, `.m4a`:**
  * **Варіант 1:** Встановити у системний PATH (команда `winget install Gyan.FFmpeg` у терміналі).
  * **Варіант 2:** Покласти `ffmpeg.exe` у папку `.minecraft/custom_music/ffmpeg/`.

### 🔨 Збірка з вихідного коду
* Вимоги: **JDK 21**
* Команда збірки:
  ```cmd
  gradlew.bat clean build
  ```
* Готовий файл `.jar` буде в папці `build/libs/`.

### 📜 Ліцензія
Поширюється за ліцензією **MIT License**.
